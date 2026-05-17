package util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class InputValidatorTest {

  @Test
  void scenarioE_validationErrors() {
    List<InputValidator.ProcessDraft> drafts = List.of(
        new InputValidator.ProcessDraft("P1", "0", "-1"),   // burst invalid
        new InputValidator.ProcessDraft("", "0", "2"),       // id invalid
        new InputValidator.ProcessDraft("P1", "1", "3")      // duplicate id
    );

    InputValidator.ValidationResult result = InputValidator.validateForSimulation(drafts, "2");
    assertFalse(result.isValid());

    String allErrors = String.join("\n", result.getErrors());
    assertTrue(allErrors.contains("Burst Time must be > 0."));
    assertTrue(allErrors.contains("Process ID is required and cannot be empty."));
    assertTrue(allErrors.contains("Duplicate Process ID: 'P1'"));
  }

  @Test
  void addForm_rejectsDuplicateId() {
    List<String> errors =
        InputValidator.validateAddProcessForm("P1", "0", "3", "2", Set.of("P1"));
    assertFalse(errors.isEmpty());
    assertTrue(String.join("\n", errors).contains("Duplicate Process ID"));
  }

  @Test
  void addForm_rejectsEmptyFieldsAndNegative() {
    assertFalse(
        InputValidator.validateAddProcessForm("", "0", "1", "2", Set.of()).isEmpty());
    assertFalse(
        InputValidator.validateAddProcessForm("P1", "", "1", "2", Set.of()).isEmpty());
    assertFalse(
        InputValidator.validateAddProcessForm("P1", "0", "", "2", Set.of()).isEmpty());
    assertFalse(
        InputValidator.validateAddProcessForm("P1", "0", "1", "", Set.of()).isEmpty());
    List<String> negArr =
        InputValidator.validateAddProcessForm("P1", "-1", "2", "2", Set.of());
    assertTrue(String.join("\n", negArr).contains("Arrival Time cannot be negative"));
    List<String> negBurst =
        InputValidator.validateAddProcessForm("P1", "0", "-2", "2", Set.of());
    assertTrue(String.join("\n", negBurst).contains("Burst Time must be greater than 0"));
  }

  @Test
  void addForm_acceptsValidRow() {
    assertTrue(
        InputValidator.validateAddProcessForm("P2", "0", "5", "3", Set.of("P1")).isEmpty());
  }
}

