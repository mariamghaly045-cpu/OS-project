package scheduler;

import model.Process;
import model.ScheduleResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoundRobinSchedulerTest {

  @Test
  void scenarioA_mixedWorkload() {
    List<Process> processes = List.of(
        new Process("P1", 0, 6),
        new Process("P2", 1, 4),
        new Process("P3", 2, 2),
        new Process("P4", 3, 5)
    );

    Scheduler scheduler = new RoundRobinScheduler();
    ScheduleResult result = scheduler.schedule(processes, 3);

    assertEquals(7.75, result.getAverageWaitingTime(), 1e-6);
    assertEquals(12.0, result.getAverageTurnaroundTime(), 1e-6);
    assertEquals(2.75, result.getAverageResponseTime(), 1e-6);

    assertMetrics(result, "P1", 8, 14, 0);
    assertMetrics(result, "P2", 10, 14, 2);
    assertMetrics(result, "P3", 4, 6, 4);
    assertMetrics(result, "P4", 9, 14, 5);
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

    ScheduleResult result = new RoundRobinScheduler().schedule(processes, 2);
    assertEquals(1.8, result.getAverageWaitingTime(), 1e-6);
    assertEquals(3.4, result.getAverageTurnaroundTime(), 1e-6);
    assertEquals(1.6, result.getAverageResponseTime(), 1e-6);

    assertMetrics(result, "P1", 0, 1, 0);
    assertMetrics(result, "P2", 1, 3, 1);
    assertMetrics(result, "P3", 2, 3, 2);
    assertMetrics(result, "P4", 3, 6, 2);
    assertMetrics(result, "P5", 3, 4, 3);
  }

  @Test
  void scenarioC_fairness() {
    List<Process> processes = List.of(
        new Process("P1", 0, 8),
        new Process("P2", 0, 8),
        new Process("P3", 0, 8)
    );

    ScheduleResult result = new RoundRobinScheduler().schedule(processes, 2);
    assertEquals(14.0, result.getAverageWaitingTime(), 1e-6);
    assertEquals(22.0, result.getAverageTurnaroundTime(), 1e-6);
    assertEquals(2.0, result.getAverageResponseTime(), 1e-6);

    assertMetrics(result, "P1", 12, 20, 0);
    assertMetrics(result, "P2", 14, 22, 2);
    assertMetrics(result, "P3", 16, 24, 4);
  }

  @Test
  void scenarioD_longJobSensitivity() {
    List<Process> processes = List.of(
        new Process("P1", 0, 20),
        new Process("P2", 1, 2),
        new Process("P3", 2, 3),
        new Process("P4", 3, 1)
    );

    ScheduleResult result = new RoundRobinScheduler().schedule(processes, 4);
    assertEquals(4.75, result.getAverageWaitingTime(), 1e-6);
    assertEquals(11.25, result.getAverageTurnaroundTime(), 1e-6);
    assertEquals(3.25, result.getAverageResponseTime(), 1e-6);

    assertMetrics(result, "P1", 6, 26, 0);
    assertMetrics(result, "P2", 3, 5, 3);
    assertMetrics(result, "P3", 4, 7, 4);
    assertMetrics(result, "P4", 6, 7, 6);
  }

  private static void assertMetrics(ScheduleResult result, String processId, int wt, int tat, int rt) {
    ScheduleResult.ProcessMetrics pm = result.getProcessMetrics().get(processId);
    assertNotNull(pm, "Missing metrics for " + processId);
    assertEquals(wt, pm.getWaitingTime());
    assertEquals(tat, pm.getTurnaroundTime());
    assertEquals(rt, pm.getResponseTime());
  }
}

