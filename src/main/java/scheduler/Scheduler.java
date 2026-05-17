package scheduler;

import model.Process;
import model.ScheduleResult;

import java.util.Comparator;
import java.util.List;

public interface Scheduler {
  /**
   * @param processes workload; schedulers must operate on the same set of processes
   * @param timeQuantum time quantum used by Round Robin (ignored by non-preemptive algorithms)
   */
  ScheduleResult schedule(List<Process> processes, int timeQuantum);
}

