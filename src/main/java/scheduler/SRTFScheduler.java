package scheduler;

import metrics.MetricsCalculator;
import model.Process;
import model.ScheduleResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

public final class SRTFScheduler implements Scheduler {
  @Override
  public ScheduleResult schedule(List<Process> processes, int timeQuantum) {
    Objects.requireNonNull(processes, "processes");
    if (processes.isEmpty()) throw new IllegalArgumentException("processes must not be empty");

    List<Process> sortedByArrivalThenId = new ArrayList<>(processes);
    sortedByArrivalThenId.sort(
        Comparator.comparingInt(Process::getArrivalTime)
            .thenComparing(Process::getProcessId)
    );

    Map<String, Integer> remaining = new HashMap<>();
    Map<String, Boolean> started = new HashMap<>();
    for (Process p : processes) {
      remaining.put(p.getProcessId(), p.getBurstTime());
      started.put(p.getProcessId(), false);
    }

    PriorityQueue<Process> available = new PriorityQueue<>(
        Comparator.comparingInt((Process p) -> remaining.get(p.getProcessId()))
            .thenComparingInt(Process::getArrivalTime)
            .thenComparing(Process::getProcessId)
    );

    ScheduleResult result = new ScheduleResult("SRTF");
    List<ScheduleResult.GanttBlock> blocks = new ArrayList<>();

    class SegAppender {
      void append(String pid, int start, int end) {
        if (start >= end) return;
        if (blocks.isEmpty()) {
          blocks.add(new ScheduleResult.GanttBlock(pid, start, end));
          return;
        }
        ScheduleResult.GanttBlock last = blocks.get(blocks.size() - 1);
        if (Objects.equals(last.getProcessId(), pid) && last.getEndTime() == start) {
          blocks.remove(blocks.size() - 1);
          blocks.add(new ScheduleResult.GanttBlock(pid, last.getStartTime(), end));
          return;
        }
        blocks.add(new ScheduleResult.GanttBlock(pid, start, end));
      }
    }
    SegAppender seg = new SegAppender();

    int currentTime = 0;
    int arrivalIdx = 0;
    int completed = 0;
    int n = processes.size();

    while (completed < n) {
      while (arrivalIdx < n && sortedByArrivalThenId.get(arrivalIdx).getArrivalTime() <= currentTime) {
        available.add(sortedByArrivalThenId.get(arrivalIdx));
        arrivalIdx++;
      }

      if (available.isEmpty()) {
        if (arrivalIdx >= n) break;
        int nextArrival = sortedByArrivalThenId.get(arrivalIdx).getArrivalTime();
        if (currentTime < nextArrival) {
          seg.append(null, currentTime, nextArrival);
          currentTime = nextArrival;
        }
        continue;
      }

      Process running = available.poll();
      String pid = running.getProcessId();
      if (!started.get(pid)) {
        started.put(pid, true);
        result.setFirstStartTime(pid, currentTime);
      }

      seg.append(pid, currentTime, currentTime + 1);
      remaining.put(pid, remaining.get(pid) - 1);
      currentTime++;

      if (remaining.get(pid) == 0) {
        result.setCompletionTime(pid, currentTime);
        completed++;
      } else {
        available.add(running);
      }
    }

    for (ScheduleResult.GanttBlock b : blocks) {
      result.addGanttBlock(b);
    }
    MetricsCalculator.populateMetrics(result, processes);
    return result;
  }
}
