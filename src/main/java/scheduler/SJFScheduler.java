package scheduler;

import model.Process;
import model.ScheduleResult;

import metrics.MetricsCalculator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

public final class SJFScheduler implements Scheduler {
  @Override
  public ScheduleResult schedule(List<Process> processes, int timeQuantum) {
    Objects.requireNonNull(processes, "processes");
    if (processes.isEmpty()) throw new IllegalArgumentException("processes must not be empty");

    // SJF is non-preemptive: timeQuantum is ignored.

    List<Process> sortedByArrivalThenId = new ArrayList<>(processes);
    sortedByArrivalThenId.sort(
        Comparator.comparingInt(Process::getArrivalTime)
            .thenComparing(Process::getProcessId)
    );

    PriorityQueue<Process> available = new PriorityQueue<>(
        Comparator.comparingInt(Process::getBurstTime)
            .thenComparingInt(Process::getArrivalTime)
            .thenComparing(Process::getProcessId)
    );

    Map<String, Boolean> started = new HashMap<>();
    for (Process p : processes) started.put(p.getProcessId(), false);

    ScheduleResult result = new ScheduleResult("SJF");

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
        } else {
          blocks.add(new ScheduleResult.GanttBlock(pid, start, end));
        }
      }
    }
    SegAppender seg = new SegAppender();

    int currentTime = 0;
    int arrivalIdx = 0;
    int completed = 0;
    int n = processes.size();

    while (completed < n) {
      // Add all processes that have arrived by currentTime.
      while (arrivalIdx < n && sortedByArrivalThenId.get(arrivalIdx).getArrivalTime() <= currentTime) {
        available.add(sortedByArrivalThenId.get(arrivalIdx));
        arrivalIdx++;
      }

      if (available.isEmpty()) {
        if (arrivalIdx >= n) break;
        int nextArrival = sortedByArrivalThenId.get(arrivalIdx).getArrivalTime();
        if (currentTime < nextArrival) {
          seg.append(null, currentTime, nextArrival); // Idle
          currentTime = nextArrival;
        }
        continue;
      }

      Process running = available.poll();
      int start = currentTime;
      int completion = start + running.getBurstTime();

      if (!started.get(running.getProcessId())) {
        started.put(running.getProcessId(), true);
        result.setFirstStartTime(running.getProcessId(), start);
      }

      seg.append(running.getProcessId(), start, completion);
      result.setCompletionTime(running.getProcessId(), completion);

      currentTime = completion;
      completed++;
    }

    for (ScheduleResult.GanttBlock b : blocks) {
      result.addGanttBlock(b);
    }

    MetricsCalculator.populateMetrics(result, processes);
    return result;
  }
}
