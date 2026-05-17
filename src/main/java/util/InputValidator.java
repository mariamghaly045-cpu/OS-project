package util;

import model.Process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class InputValidator {
  private InputValidator() {}

  public static final class ProcessDraft {
    private final String processId;
    private final String arrivalTime;
    private final String burstTime;

    public ProcessDraft(String processId, String arrivalTime, String burstTime) {
      this.processId = processId;
      this.arrivalTime = arrivalTime;
      this.burstTime = burstTime;
    }

    public String getProcessId() {
      return processId;
    }

    public String getArrivalTime() {
      return arrivalTime;
    }

    public String getBurstTime() {
      return burstTime;
    }
  }

  public static final class ValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final int timeQuantum;
    private final List<Process> processes;

    private ValidationResult(boolean valid, List<String> errors, int timeQuantum, List<Process> processes) {
      this.valid = valid;
      this.errors = errors;
      this.timeQuantum = timeQuantum;
      this.processes = processes;
    }

    public boolean isValid() {
      return valid;
    }

    public List<String> getErrors() {
      return Collections.unmodifiableList(errors);
    }

    public int getTimeQuantum() {
      return timeQuantum;
    }

    public List<Process> getProcesses() {
      return Collections.unmodifiableList(processes);
    }
  }

  public static ValidationResult validateForSimulation(List<ProcessDraft> drafts, String timeQuantumText) {
    List<String> errors = new ArrayList<>();

    if (drafts == null || drafts.isEmpty()) {
      errors.add("Please add at least 1 process.");
    }

    Integer quantum = null;
    if (timeQuantumText == null || timeQuantumText.isBlank()) {
      errors.add("Time Quantum is required.");
    } else {
      try {
        quantum = Integer.parseInt(timeQuantumText.trim());
        if (quantum <= 0) errors.add("Time Quantum must be a positive integer (> 0).");
      } catch (NumberFormatException e) {
        errors.add("Time Quantum must be an integer (> 0).");
      }
    }

    List<Process> processes = new ArrayList<>();
    Set<String> seenIds = new HashSet<>();

    if (drafts != null) {
      for (int i = 0; i < drafts.size(); i++) {
        ProcessDraft d = drafts.get(i);
        int row = i + 1;

        String id = safeTrim(d.getProcessId());
        String arrivalText = safeTrim(d.getArrivalTime());
        String burstText = safeTrim(d.getBurstTime());

        if (id == null) {
          errors.add("Row " + row + ": Process ID is required and cannot be empty.");
          continue;
        }

        // Track duplicates even if other fields are invalid, so the user sees all ID issues at once.
        if (seenIds.contains(id)) {
          errors.add("Duplicate Process ID: '" + id + "'. IDs must be unique.");
          continue;
        }
        seenIds.add(id);

        if (arrivalText == null) {
          errors.add("Row " + row + " (" + id + "): Arrival Time is required.");
          continue;
        }
        if (burstText == null) {
          errors.add("Row " + row + " (" + id + "): Burst Time is required.");
          continue;
        }

        Integer arrival = null;
        Integer burst = null;
        try {
          arrival = Integer.parseInt(arrivalText);
        } catch (NumberFormatException e) {
          errors.add("Row " + row + " (" + id + "): Arrival Time must be a non-negative integer.");
        }

        try {
          burst = Integer.parseInt(burstText);
        } catch (NumberFormatException e) {
          errors.add("Row " + row + " (" + id + "): Burst Time must be an integer (> 0).");
        }

        if (arrival != null && arrival < 0) {
          errors.add("Row " + row + " (" + id + "): Arrival Time cannot be negative.");
        }
        if (burst != null && burst <= 0) {
          errors.add("Row " + row + " (" + id + "): Burst Time must be > 0.");
        }

        if (arrival != null && burst != null && arrival >= 0 && burst > 0) {
          processes.add(new Process(id, arrival, burst));
        }
      }
    }

    boolean valid = errors.isEmpty() && quantum != null && !processes.isEmpty();
    return new ValidationResult(valid, errors, quantum == null ? -1 : quantum, processes);
  }

  /**
   * Validates the input panel before appending a row (coversheet: no empty fields, no negatives
   * where disallowed, no duplicate IDs vs. {@code existingTrimmedIds}, valid time quantum).
   */
  public static List<String> validateAddProcessForm(
      String processIdRaw,
      String arrivalRaw,
      String burstRaw,
      String timeQuantumText,
      Set<String> existingTrimmedIds) {

    List<String> errors = new ArrayList<>();
    Objects.requireNonNull(existingTrimmedIds, "existingTrimmedIds");

    String id = safeTrim(processIdRaw);
    if (id == null) {
      errors.add("Process ID is required and cannot be empty.");
    } else if (existingTrimmedIds.contains(id)) {
      errors.add("Duplicate Process ID: '" + id + "'. Each process must have a unique ID.");
    }

    String arrivalText = safeTrim(arrivalRaw);
    if (arrivalText == null) {
      errors.add("Arrival Time is required.");
    }

    String burstText = safeTrim(burstRaw);
    if (burstText == null) {
      errors.add("Burst Time is required.");
    }

    if (timeQuantumText == null || timeQuantumText.isBlank()) {
      errors.add("Time Quantum is required.");
    } else {
      try {
        int q = Integer.parseInt(timeQuantumText.trim());
        if (q <= 0) {
          errors.add("Time Quantum must be a positive integer (> 0).");
        }
      } catch (NumberFormatException e) {
        errors.add("Time Quantum must be a valid integer.");
      }
    }

    if (arrivalText != null) {
      try {
        int arrival = Integer.parseInt(arrivalText);
        if (arrival < 0) {
          errors.add("Arrival Time cannot be negative.");
        }
      } catch (NumberFormatException e) {
        errors.add("Arrival Time must be a non-negative integer.");
      }
    }

    if (burstText != null) {
      try {
        int burst = Integer.parseInt(burstText);
        if (burst <= 0) {
          errors.add("Burst Time must be greater than 0.");
        }
      } catch (NumberFormatException e) {
        errors.add("Burst Time must be a valid integer.");
      }
    }

    return errors;
  }

  private static String safeTrim(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}

