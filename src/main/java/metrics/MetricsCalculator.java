package metrics;

import model.Process;
import model.ScheduleResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MetricsCalculator {
  private MetricsCalculator() {}

  public static void populateMetrics(ScheduleResult result, List<Process> processes) {
    Map<String, Process> byId = new HashMap<>();
    for (Process p : processes) byId.put(p.getProcessId(), p);

    double sumWT = 0;
    double sumTAT = 0;
    double sumRT = 0;

    for (Process p : processes) {
      int completion = result.getCompletionTimes().get(p.getProcessId());
      int firstStart = result.getFirstStartTimes().get(p.getProcessId());

      int turnaroundTime = completion - p.getArrivalTime();
      int waitingTime = turnaroundTime - p.getBurstTime();
      int responseTime = firstStart - p.getArrivalTime();

      result.setProcessMetrics(
          p.getProcessId(),
          new ScheduleResult.ProcessMetrics(p.getProcessId(), waitingTime, turnaroundTime, responseTime)
      );

      sumWT += waitingTime;
      sumTAT += turnaroundTime;
      sumRT += responseTime;
    }

    int n = Math.max(processes.size(), 1);
    result.setAverages(
        round2(sumWT / n),
        round2(sumTAT / n),
        round2(sumRT / n)
    );
  }

  private static double round2(double v) {
    return Math.round(v * 100.0) / 100.0;
  }
}
