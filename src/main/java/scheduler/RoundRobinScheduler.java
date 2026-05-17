package scheduler;

import model.Process;
import model.ScheduleResult;

import metrics.MetricsCalculator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RoundRobinScheduler implements Scheduler {
  @Override
  public ScheduleResult schedule(List<Process> processes, int timeQuantum) {
    if (timeQuantum <= 0) throw new IllegalArgumentException("timeQuantum must be > 0");
    Objects.requireNonNull(processes, "processes");
    if (processes.isEmpty()) throw new IllegalArgumentException("processes must not be empty");

    List<Process> sortedByArrivalThenId = new ArrayList<>(processes);
    sortedByArrivalThenId.sort(
        Comparator.comparingInt(Process::getArrivalTime)
            .thenComparing(Process::getProcessId)
    );

    Map<String, Integer> remaining = new HashMap<>();
    for (Process p : processes) remaining.put(p.getProcessId(), p.getBurstTime());

    Map<String, Boolean> started = new HashMap<>();
    for (Process p : processes) started.put(p.getProcessId(), false);

    ScheduleResult result = new ScheduleResult("Round Robin");

    // RR state
    Deque<Process> readyQueue = new ArrayDeque<>();
    int arrivalIdx = 0;
    int completed = 0;
    int currentTime = 0;

    List<ScheduleResult.GanttBlock> blocks = new ArrayList<>();

    // Local append for [start,end)
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

    while (completed < processes.size()) {
      if (readyQueue.isEmpty()) {
        if (arrivalIdx >= processes.size()) break;
        int nextArrival = sortedByArrivalThenId.get(arrivalIdx).getArrivalTime();
        if (currentTime < nextArrival) {
          seg.append(null, currentTime, nextArrival); // Idle
          // RR ready-queue view: queue is empty during idle ticks
          for (int t = currentTime; t < nextArrival; t++) {
            result.addReadyQueueSnapshot(new ScheduleResult.ReadyQueueSnapshot(t, List.of()));
          }
          currentTime = nextArrival;
        }
      }

      // Enqueue arrivals at currentTime (arrival-time order, then processId)
      while (arrivalIdx < sortedByArrivalThenId.size()
          && sortedByArrivalThenId.get(arrivalIdx).getArrivalTime() == currentTime) {
        readyQueue.addLast(sortedByArrivalThenId.get(arrivalIdx));
        arrivalIdx++;
      }

      if (readyQueue.isEmpty()) continue;

      Process running = readyQueue.removeFirst();
      int runningRemaining = remaining.get(running.getProcessId());

      if (!started.get(running.getProcessId())) {
        started.put(running.getProcessId(), true);
        result.setFirstStartTime(running.getProcessId(), currentTime);
      }

      int quantumRemaining = timeQuantum;
      while (quantumRemaining > 0 && runningRemaining > 0) {
        // Snapshot at start of this time unit: [CPU process] + [ready queue in order].
        // This matches the usual mental model: who runs now, then who waits in the RR queue.
        List<String> snapshotIds = new ArrayList<>(1 + readyQueue.size());
        snapshotIds.add(running.getProcessId());
        for (Process p : readyQueue) {
          snapshotIds.add(p.getProcessId());
        }
        result.addReadyQueueSnapshot(
            new ScheduleResult.ReadyQueueSnapshot(currentTime, snapshotIds));

        int start = currentTime;
        int end = currentTime + 1;
        seg.append(running.getProcessId(), start, end);

        // Execute 1 time unit
        runningRemaining--;
        quantumRemaining--;
        currentTime = end;

        // Arrivals at this boundary join the queue before the next unit
        while (arrivalIdx < sortedByArrivalThenId.size()
            && sortedByArrivalThenId.get(arrivalIdx).getArrivalTime() == currentTime) {
          readyQueue.addLast(sortedByArrivalThenId.get(arrivalIdx));
          arrivalIdx++;
        }
      }

      remaining.put(running.getProcessId(), runningRemaining);
      if (runningRemaining == 0) {
        completed++;
        result.setCompletionTime(running.getProcessId(), currentTime);
      } else {
        // Quantum expired: re-enqueue at end
        readyQueue.addLast(running);
      }
    }

    for (ScheduleResult.GanttBlock b : blocks) {
      result.addGanttBlock(b);
    }
    MetricsCalculator.populateMetrics(result, processes);
    return result;
  }
}
