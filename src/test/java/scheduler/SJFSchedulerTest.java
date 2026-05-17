package scheduler;

import model.Process;
import model.ScheduleResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SJFSchedulerTest {

  @Test
  void scenarioA_mixedWorkload() {
    List<Process> processes = List.of(
        new Process("P1", 0, 6),
        new Process("P2", 1, 4),
        new Process("P3", 2, 2),
        new Process("P4", 3, 5)
    );

    ScheduleResult result = new SJFScheduler().schedule(processes, 3);

    assertEquals(5.0, result.getAverageWaitingTime(), 1e-6);
    assertEquals(9.25, result.getAverageTurnaroundTime(), 1e-6);
    assertEquals(5.0, result.getAverageResponseTime(), 1e-6);

    assertMetrics(result, "P1", 0, 6, 0);
    assertMetrics(result, "P2", 7, 11, 7);
    assertMetrics(result, "P3", 4, 6, 4);
    assertMetrics(result, "P4", 9, 14, 9);
  }

  @Test
  void scenarioB_shortJobHeavy() {
    List<Process> processes = List.of(
        new Process("P1", 0, 1),
        new Process("P2", 0, 2),
        new Process("P3", 1, 1),
        new Process("P4", 2, 3),
        new Process("P5", 3, 1)
    );

    ScheduleResult result = new SJFScheduler().schedule(processes, 2);

    assertEquals(1.2, result.getAverageWaitingTime(), 1e-6);
    assertEquals(2.8, result.getAverageTurnaroundTime(), 1e-6);
    assertEquals(1.2, result.getAverageResponseTime(), 1e-6);

    assertMetrics(result, "P1", 0, 1, 0);
    assertMetrics(result, "P2", 2, 4, 2);
    assertMetrics(result, "P3", 0, 1, 0);
    assertMetrics(result, "P4", 3, 6, 3);
    assertMetrics(result, "P5", 1, 2, 1);
  }

  @Test
  void scenarioC_fairness() {
    List<Process> processes = List.of(
        new Process("P1", 0, 8),
        new Process("P2", 0, 8),
        new Process("P3", 0, 8)
    );

    ScheduleResult result = new SJFScheduler().schedule(processes, 2);

    assertEquals(8.0, result.getAverageWaitingTime(), 1e-6);
    assertEquals(16.0, result.getAverageTurnaroundTime(), 1e-6);
    assertEquals(8.0, result.getAverageResponseTime(), 1e-6);

    assertMetrics(result, "P1", 0, 8, 0);
    assertMetrics(result, "P2", 8, 16, 8);
    assertMetrics(result, "P3", 16, 24, 16);
  }

  @Test
  void scenarioD_longJobSensitivity() {
    List<Process> processes = List.of(
        new Process("P1", 0, 20),
        new Process("P2", 1, 2),
        new Process("P3", 2, 3),
        new Process("P4", 3, 1)
    );

    ScheduleResult result = new SJFScheduler().schedule(processes, 4);

    assertEquals(14.5, result.getAverageWaitingTime(), 1e-6);
    assertEquals(21.0, result.getAverageTurnaroundTime(), 1e-6);
    assertEquals(14.5, result.getAverageResponseTime(), 1e-6);

    assertMetrics(result, "P1", 0, 20, 0);
    assertMetrics(result, "P2", 20, 22, 20);
    assertMetrics(result, "P3", 21, 24, 21);
    assertMetrics(result, "P4", 17, 18, 17);
  }

  private static void assertMetrics(ScheduleResult result, String processId, int wt, int tat, int rt) {
    ScheduleResult.ProcessMetrics pm = result.getProcessMetrics().get(processId);
    assertNotNull(pm, "Missing metrics for " + processId);
    assertEquals(wt, pm.getWaitingTime());
    assertEquals(tat, pm.getTurnaroundTime());
    assertEquals(rt, pm.getResponseTime());
  }
}

