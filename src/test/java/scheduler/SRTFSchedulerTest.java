package scheduler;

import model.Process;
import model.ScheduleResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SRTFSchedulerTest {

  @Test
  void preemptionProducesExpectedMetrics() {
    List<Process> processes = List.of(
        new Process("P1", 0, 8),
        new Process("P2", 1, 4),
        new Process("P3", 2, 2)
    );

    ScheduleResult result = new SRTFScheduler().schedule(processes, 3);

    assertEquals(2.67, result.getAverageWaitingTime(), 1e-6);
    assertEquals(7.33, result.getAverageTurnaroundTime(), 1e-6);
    assertEquals(0.0, result.getAverageResponseTime(), 1e-6);

    assertMetrics(result, "P1", 6, 14, 0);
    assertMetrics(result, "P2", 2, 6, 0);
    assertMetrics(result, "P3", 0, 2, 0);
  }

  private static void assertMetrics(ScheduleResult result, String processId, int wt, int tat, int rt) {
    ScheduleResult.ProcessMetrics pm = result.getProcessMetrics().get(processId);
    assertNotNull(pm, "Missing metrics for " + processId);
    assertEquals(wt, pm.getWaitingTime());
    assertEquals(tat, pm.getTurnaroundTime());
    assertEquals(rt, pm.getResponseTime());
  }
}
