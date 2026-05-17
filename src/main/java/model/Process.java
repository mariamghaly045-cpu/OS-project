package model;

import java.util.Objects;

public final class Process {
  private final String processId;
  private final int arrivalTime;
  private final int burstTime;

  public Process(String processId, int arrivalTime, int burstTime) {
    this.processId = Objects.requireNonNull(processId, "processId");
    this.arrivalTime = arrivalTime;
    this.burstTime = burstTime;
  }

  public String getProcessId() {
    return processId;
  }

  public int getArrivalTime() {
    return arrivalTime;
  }

  public int getBurstTime() {
    return burstTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Process)) return false;
    Process process = (Process) o;
    return processId.equals(process.processId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(processId);
  }

  @Override
  public String toString() {
    return processId + "(arr=" + arrivalTime + ", burst=" + burstTime + ")";
  }
}

