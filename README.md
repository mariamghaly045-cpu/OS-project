# CPU Scheduling Comparator — Round Robin / SJF / SRTF

An **Operating Systems** style project that simulates and compares **three** CPU scheduling algorithms on the **same workload**: **Round Robin (RR)**, **Shortest Job First (SJF, non-preemptive)**, and **Shortest Remaining Time First (SRTF, preemptive)**.





## What This Project Does

The desktop GUI accepts dynamic process input (or preset scenarios), runs all three schedulers, and shows:

- **RR ready queue timeline** — CPU vs ready queue per tick (Round Robin only)
- Three side-by-side **Gantt charts** (RR, SJF, SRTF)
- Per-process tables: **Waiting Time (WT)**, **Turnaround Time (TAT)**, **Response Time (RT)** plus row averages
- **Comparison summary** table — averages per algorithm with a **Winner** column (lowest average wins per metric)
- **Required Analysis / Required Conclusion** — generated text after simulation (preset wording for scenarios A–D when the workload matches)
- **Input validation** — duplicate IDs, empty fields, negative numbers, invalid quantum (see coversheet rules)


There is **no separate console simulator** in this repository; use Maven to launch the GUI.

---

## Algorithms

### Round Robin (RR)

Each ready process gets the CPU for up to **time quantum** units. If the burst does not finish, the process goes to the **back of the ready queue**. Idle time appears as a grey block when no process is ready.

### Shortest Job First (SJF)

**Non-preemptive.** When the CPU becomes idle and multiple processes have arrived, pick the one with the **smallest total burst time** (ties: earlier arrival, then process ID). The quantum field is **ignored**.

### Shortest Remaining Time First (SRTF)

**Preemptive.** At each time unit, among arrived processes with work left, run the one with the **smallest remaining burst**. Equivalent to “preemptive SJF”. Quantum is **ignored**.

---

## Metrics

| Metric | Formula |
| ------ | ------- |
| Turnaround Time (TAT) | Completion time − Arrival time |
| Waiting Time (WT) | TAT − Burst time |
| Response Time (RT) | First time on CPU − Arrival time |

All times use **integer** simulation clocks.

---

## Requirements

| Tool | Version (this project) |
| ---- | ------------------------ |
| JDK | **17** (`maven.compiler.release`) |
| JavaFX | **21.0.4** |
| Maven | 3.8+ recommended |
| OS | Windows / macOS / Linux |

---

## Build & Run

From the project directory (the folder containing `pom.xml`):

```bash
mvn clean test
```

Run the GUI:

```bash
mvn javafx:run
```

Main class: `gui.MainApp`.

On Windows, if the path contains spaces, run Maven from that directory or quote paths.

---

## Preset Scenarios (GUI dropdown)

| Scenario | Role | Typical workload (see `test-cases/`) |
| -------- | ---- | -------------------------------------- |
| **A** | Basic mixed workload | Four processes, staggered arrivals; quantum **3** |
| **B** | Short-job-heavy | Five processes; quantum **2** |
| **C** | Fairness | Three equal bursts, same arrival pattern; quantum **2** |
| **D** | Long-job sensitivity | One long burst vs shorter jobs; quantum **4** |
| **E** | Validation | Intentionally invalid rows + bad quantum — demonstrates error messages |

Exact numbers match `test-cases/scenario-A.txt` … `scenario-E.txt`.

---

## Project Layout

```
src/main/java/
├── gui/           # JavaFX UI (MainApp, MainController, GanttChartPanel, ResultsTable, …)
├── scheduler/     # RoundRobinScheduler, SJFScheduler, SRTFScheduler, Scheduler
├── model/         # Process, ScheduleResult
├── metrics/       # MetricsCalculator (WT / TAT / RT averages)
├── util/          # InputValidator
└── doc/           # Optional POI-based Word helper (see pom exec plugin)

src/test/java/
└── scheduler/ … util/   # JUnit tests

test-cases/
├── scenario-A.txt … scenario-E.txt   # Reference inputs / expected notes
assets/screenshots/                   # Placeholder SVGs for docs
```

---

## Assumptions & Tie-Breaking

- Time is discrete (integer ticks).
- RR uses a single ready queue; ordering follows implementation (arrival + FCFS-style ordering for ties).
- SJF queue order among equal bursts: **earlier arrival**, then **process ID**.
- SRTF uses **remaining time** at each step with the same tie-breaking idea as implemented in code.
- Very large schedules may produce long RR queue lists in the UI.
