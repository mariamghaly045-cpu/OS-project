package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ScheduleResult {
  private final String algorithmName;

  private final List<GanttBlock> ganttBlocks = new ArrayList<>();
  private final Map<String, Integer> completionTimes = new HashMap<>();
  private final Map<String, Integer> firstStartTimes = new HashMap<>();

  // RR-only
  private final List<ReadyQueueSnapshot> readyQueueSnapshots = new ArrayList<>();

  // Filled by MetricsCalculator
  private final Map<String, ProcessMetrics> processMetrics = new HashMap<>();
  private double averageWaitingTime;
  private double averageTurnaroundTime;
  private double averageResponseTime;

  public ScheduleResult(String algorithmName) {
    this.algorithmName = Objects.requireNonNull(algorithmName, "algorithmName");
  }

  public String getAlgorithmName() {
    return algorithmName;
  }

  public List<GanttBlock> getGanttBlocks() {
    return Collections.unmodifiableList(ganttBlocks);
  }

  public Map<String, Integer> getCompletionTimes() {
    return Collections.unmodifiableMap(completionTimes);
  }

  public Map<String, Integer> getFirstStartTimes() {
    return Collections.unmodifiableMap(firstStartTimes);
  }

  public List<ReadyQueueSnapshot> getReadyQueueSnapshots() {
    return Collections.unmodifiableList(readyQueueSnapshots);
  }

  public Map<String, ProcessMetrics> getProcessMetrics() {
    return Collections.unmodifiableMap(processMetrics);
  }

  public double getAverageWaitingTime() {
    return averageWaitingTime;
  }

  public double getAverageTurnaroundTime() {
    return averageTurnaroundTime;
  }

  public double getAverageResponseTime() {
    return averageResponseTime;
  }

  public void addGanttBlock(GanttBlock block) {
    ganttBlocks.add(Objects.requireNonNull(block, "block"));
  }

  public void setCompletionTime(String processId, int completionTime) {
    completionTimes.put(processId, completionTime);
  }

  public void setFirstStartTime(String processId, int firstStartTime) {
    firstStartTimes.put(processId, firstStartTime);
  }

  public void addReadyQueueSnapshot(ReadyQueueSnapshot snapshot) {
    readyQueueSnapshots.add(Objects.requireNonNull(snapshot, "snapshot"));
  }

  public void setProcessMetrics(String processId, ProcessMetrics metrics) {
    processMetrics.put(processId, metrics);
  }

  public void setAverages(double averageWaitingTime, double averageTurnaroundTime, double averageResponseTime) {
    this.averageWaitingTime = averageWaitingTime;
    this.averageTurnaroundTime = averageTurnaroundTime;
    this.averageResponseTime = averageResponseTime;
  }

  public int getTotalTime() {
    if (ganttBlocks.isEmpty()) return 0;
    return ganttBlocks.get(ganttBlocks.size() - 1).getEndTime();
  }

  public static final class GanttBlock {
    private final String processId; // null => Idle
    private final int startTime;
    private final int endTime;

    public GanttBlock(String processId, int startTime, int endTime) {
      this.processId = processId;
      this.startTime = startTime;
      this.endTime = endTime;
    }

    public String getProcessId() {
      return processId;
    }

    public int getStartTime() {
      return startTime;
    }

    public int getEndTime() {
      return endTime;
    }

    public boolean isIdle() {
      return processId == null;
    }

    public String getDisplayLabel() {
      return isIdle() ? "Idle" : processId;
    }
  }

  public static final class ReadyQueueSnapshot {
    private final int timeTick;
    private final List<String> queueProcessIds;

    public ReadyQueueSnapshot(int timeTick, List<String> queueProcessIds) {
      this.timeTick = timeTick;
      this.queueProcessIds = List.copyOf(queueProcessIds);
    }

    public int getTimeTick() {
      return timeTick;
    }

    public List<String> getQueueProcessIds() {
      return queueProcessIds;
    }
  }

  public static final class ProcessMetrics {
    private final String processId;
    private final int waitingTime;
    private final int turnaroundTime;
    private final int responseTime;

    public ProcessMetrics(String processId, int waitingTime, int turnaroundTime, int responseTime) {
      this.processId = processId;
      this.waitingTime = waitingTime;
      this.turnaroundTime = turnaroundTime;
      this.responseTime = responseTime;
    }

    public String getProcessId() {
      return processId;
    }

    public int getWaitingTime() {
      return waitingTime;
    }

    public int getTurnaroundTime() {
      return turnaroundTime;
    }

    public int getResponseTime() {
      return responseTime;
    }
  }
}

