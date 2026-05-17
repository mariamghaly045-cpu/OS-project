package util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Maintains {@code simulation-inputs.log} as a live mirror of what appears in the simulator GUI:
 * top fields, preset scenario selection, process table (manual vs preset rows), and optional last
 * successful run summary. The file is rewritten on each relevant change so removed UI state is not
 * left in the file.
 */
public final class InputLogWriter {

  private static final Path LOG_FILE = Paths.get("simulation-inputs.log");
  private static final DateTimeFormatter TS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private InputLogWriter() {}

  /** One row in the process table snapshot. */
  public static final class TableRowLine {
    public final String processId;
    public final String arrival;
    public final String burst;
    /** {@code true} if the row was added with "Add Process"; {@code false} if loaded from a preset scenario. */
    public final boolean fromAddProcessButton;

    public TableRowLine(
        String processId,
        String arrival,
        String burst,
        boolean fromAddProcessButton) {
      this.processId = processId == null ? "" : processId;
      this.arrival = arrival == null ? "" : arrival;
      this.burst = burst == null ? "" : burst;
      this.fromAddProcessButton = fromAddProcessButton;
    }
  }

  /**
   * Rewrites the entire log file to match the current GUI snapshot.
   *
   * @param lastSimulationSection optional multi-line block for the last successful run; omit or
   *     pass {@code null} if none (e.g. after Clear / Reset / before any run).
   */
  public static void rewriteSimulationInputsLog(
      String processIdField,
      String arrivalField,
      String burstField,
      String quantumField,
      String presetScenarioDisplay,
      List<TableRowLine> tableRows,
      String lastSimulationSection) {
    try {
      String ls = System.lineSeparator();
      StringBuilder sb = new StringBuilder();
      sb.append("# simulation-inputs.log — full snapshot (updates when you change the GUI)")
          .append(ls);
      sb.append("# Updated: ").append(LocalDateTime.now().format(TS)).append(ls);
      sb.append(ls);

      sb.append("=== Input form (fields above the table) ===").append(ls);
      sb.append("Process ID: ").append(nullToEmpty(processIdField)).append(ls);
      sb.append("Arrival Time: ").append(nullToEmpty(arrivalField)).append(ls);
      sb.append("Burst Time: ").append(nullToEmpty(burstField)).append(ls);
      sb.append("Time Quantum (RR): ").append(nullToEmpty(quantumField)).append(ls);
      sb.append(ls);

      sb.append("=== Preset scenario (combo) ===").append(ls);
      sb.append(nullToEmpty(presetScenarioDisplay)).append(ls);
      sb.append(ls);

      sb.append("=== Process table ===").append(ls);
      if (tableRows == null || tableRows.isEmpty()) {
        sb.append("(empty — no processes in the table)").append(ls);
      } else {
        int i = 1;
        for (TableRowLine r : tableRows) {
          String tag = r.fromAddProcessButton ? "manual (Add Process)" : "from preset scenario";
          sb.append(i++)
              .append(". ID=")
              .append(r.processId)
              .append(" | Arrival=")
              .append(r.arrival)
              .append(" | Burst=")
              .append(r.burst)
              .append(" | ")
              .append(tag)
              .append(ls);
        }
      }
      sb.append(ls);

      sb.append("=== Last successful simulation ===").append(ls);
      if (lastSimulationSection == null || lastSimulationSection.isBlank()) {
        sb.append("(none — run simulation or outputs were cleared)").append(ls);
      } else {
        sb.append(lastSimulationSection.trim()).append(ls);
      }

      Files.writeString(
          LOG_FILE,
          sb.toString(),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (Exception e) {
      System.err.println("simulation-inputs.log: " + e.getMessage());
    }
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
