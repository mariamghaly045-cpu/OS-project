package gui;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import model.Process;
import model.ScheduleResult;
import scheduler.RoundRobinScheduler;
import scheduler.SRTFScheduler;
import scheduler.SJFScheduler;
import util.InputLogWriter;
import util.InputValidator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static util.InputValidator.ProcessDraft;

public final class MainController {

  /** Table body grows with row count; cap height so many processes still scroll inside the table. */
  private static final double PROCESS_TABLE_ROW_HEIGHT = 27.0;
  private static final double PROCESS_TABLE_HEADER_HEIGHT = 32.0;
  private static final int PROCESS_TABLE_MIN_VISIBLE_ROWS = 4;
  private static final double PROCESS_TABLE_MAX_VIEW_HEIGHT = 720.0;

  /** Preset text when the workload matches Scenario A (Basic mixed workload). */
  private static final String REQUIRED_ANALYSIS_SCENARIO_A =
      "Required Analysis Questions\n\n"
          + "1. Which algorithm gave lower average waiting time?\n\n"
          + "SRTF gave the lowest average waiting time (4.25).\n\n"
          + "2. Which algorithm gave lower average response time?\n\n"
          + "SRTF gave the lowest average response time (2.25).\n\n"
          + "3. Did Round Robin appear fairer across all processes?\n\n"
          + "Yes. Round Robin appeared fairer because CPU time was shared between all processes more evenly.\n\n"
          + "4. Did SJF complete short jobs more efficiently?\n\n"
          + "Yes. SJF completed short jobs faster and reduced waiting time for shorter processes.\n\n"
          + "5. How did the chosen quantum affect Round Robin behavior?\n\n"
          + "The quantum (3) caused frequent switching between processes, improving fairness and responsiveness but increasing waiting time compared to SJF and SRTF.\n\n"
          + "6. Which algorithm would you recommend for the tested workload, and why?\n\n"
          + "I would recommend SRTF because it achieved the best overall performance with the lowest waiting time, response time, and turnaround time.\n";

  private static final String REQUIRED_CONCLUSION_SCENARIO_A =
      "Required Conclusion\n\n"
          + "• State which algorithm performed better on each major metric.\n\n"
          + "SRTF performed best in waiting time, response time, and turnaround time.\n\n"
          + "• State whether Round Robin appeared more balanced.\n\n"
          + "Yes. Round Robin appeared more balanced because it distributed CPU time fairly among processes.\n\n"
          + "• State whether SJF appeared more efficient.\n\n"
          + "Yes. SJF appeared more efficient for short processes and reduced average waiting time compared to Round Robin.\n\n"
          + "• Explain the observed effect of the selected quantum.\n\n"
          + "The selected quantum improved fairness and responsiveness in Round Robin.\n";

  /** Preset text when the workload matches Scenario B (Short-job-heavy case). */
  private static final String REQUIRED_ANALYSIS_SCENARIO_B =
      "Required Analysis Questions\n\n"
          + "1. Which algorithm gave lower average waiting time?\n\n"
          + "SJF and SRTF gave the lowest average waiting time (1.20).\n\n"
          + "2. Which algorithm gave lower average response time?\n\n"
          + "SJF and SRTF gave the lowest average response time (1.20).\n\n"
          + "3. Did Round Robin appear fairer across all processes?\n\n"
          + "Yes. Round Robin appeared fairer because CPU time was shared more evenly among all processes.\n\n"
          + "4. Did SJF complete short jobs more efficiently?\n\n"
          + "Yes. SJF completed short jobs earlier and achieved lower waiting and turnaround times than Round Robin.\n\n"
          + "5. How did the chosen quantum affect Round Robin behavior?\n\n"
          + "The selected quantum caused processes to switch frequently, improving fairness and responsiveness, but slightly increasing waiting and turnaround times.\n\n"
          + "6. Which algorithm would you recommend for the tested workload, and why?\n\n"
          + "I would recommend SJF or SRTF because both achieved the best overall performance with the lowest waiting time, turnaround time, and response time.\n";

  private static final String REQUIRED_CONCLUSION_SCENARIO_B =
      "Required Conclusion\n\n"
          + "• State which algorithm performed better on each major metric.\n\n"
          + "SJF and SRTF performed best in average waiting time, turnaround time, and response time.\n\n"
          + "• State whether Round Robin appeared more balanced.\n\n"
          + "Yes. Round Robin appeared more balanced because it distributed CPU time fairly across processes.\n\n"
          + "• State whether SJF appeared more efficient.\n\n"
          + "Yes. SJF appeared more efficient for short jobs and reduced waiting and turnaround times.\n\n"
          + "• Explain the observed effect of the selected quantum.\n\n"
          + "The selected quantum improved fairness and responsiveness in Round Robin.\n";

  /** Preset text when the workload matches Scenario C (Fairness case). */
  private static final String REQUIRED_ANALYSIS_SCENARIO_C =
      "Required Analysis Questions\n\n"
          + "1. Which algorithm gave lower average waiting time?\n\n"
          + "SJF and SRTF gave the lowest average waiting time (8.00).\n\n"
          + "2. Which algorithm gave lower average response time?\n\n"
          + "Round Robin gave the lowest average response time (2.00).\n\n"
          + "3. Did Round Robin appear fairer across all processes?\n\n"
          + "Yes. Round Robin appeared fairer because each process received CPU time regularly and no process waited too long before execution.\n\n"
          + "4. Did SJF complete short jobs more efficiently?\n\n"
          + "No. In this workload, all processes had equal burst times, so SJF did not provide an advantage for short jobs.\n\n"
          + "5. How did the chosen quantum affect Round Robin behavior?\n\n"
          + "The selected quantum caused frequent switching between processes, which improved responsiveness and fairness but increased waiting and turnaround times.\n\n"
          + "6. Which algorithm would you recommend for the tested workload, and why?\n\n"
          + "I would recommend SJF or SRTF because they achieved lower waiting and turnaround times. However, Round Robin provided better responsiveness and fairness.\n";

  private static final String REQUIRED_CONCLUSION_SCENARIO_C =
      "Required Conclusion\n\n"
          + "• State which algorithm performed better on each major metric.\n\n"
          + "* SJF and SRTF performed better in waiting time and turnaround time.\n"
          + "* Round Robin performed better in response time.\n\n"
          + "• State whether Round Robin appeared more balanced.\n\n"
          + "Yes. Round Robin appeared more balanced because CPU time was distributed evenly among all processes.\n\n"
          + "• State whether SJF appeared more efficient.\n\n"
          + "No significant efficiency advantage appeared for SJF because all processes had equal burst times.\n\n"
          + "• Explain the observed effect of the selected quantum.\n\n"
          + "The selected quantum improved fairness and response time in Round Robin by allowing processes to execute repeatedly in turns, but it increased context switching and overall waiting time.\n";

  /** Preset text when the workload matches Scenario D (Long-job sensitivity). */
  private static final String REQUIRED_ANALYSIS_SCENARIO_D =
      "Required Analysis Questions\n\n"
          + "Which algorithm gave lower average waiting time?\n"
          + "→ SRTF.\n\n"
          + "Which algorithm gave lower average response time?\n"
          + "→ SRTF.\n\n"
          + "Did Round Robin appear fairer across all processes?\n"
          + "→ Yes, Round Robin showed better fairness because waiting times were more balanced across processes.\n\n"
          + "Did SJF complete short jobs more efficiently?\n"
          + "→ Yes, SJF favored short jobs and completed them faster.\n\n"
          + "How did the chosen quantum affect Round Robin behavior?\n"
          + "→ The selected quantum provided good fairness without too much context switching.\n\n"
          + "Which algorithm would you recommend for the tested workload, and why?\n"
          + "→ SRTF, because it achieved the best overall performance in WT, TAT, and RT.\n";

  private static final String REQUIRED_CONCLUSION_SCENARIO_D =
      "Required Conclusion\n\n"
          + "Better overall algorithm on this dataset: SRTF.\n\n"
          + "Metric winners: WT → SRTF, TAT → SRTF, RT → SRTF.\n\n"
          + "Round Robin appeared more balanced because CPU time was distributed more fairly across processes.\n\n"
          + "SJF appeared more efficient for short jobs, but SRTF was better overall.\n\n"
          + "The selected quantum improved fairness while keeping context switching at a reasonable level.\n";

  private final TextField processIdField = new TextField();
  private final TextField arrivalTimeField = new TextField();
  private final TextField burstTimeField = new TextField();
  private final TextField timeQuantumField = new TextField();
  private final ComboBox<String> scenarioCombo = new ComboBox<>();

  private final ObservableList<ProcessRow> processRows = FXCollections.observableArrayList();
  private final TableView<ProcessRow> processTable = new TableView<>(processRows);

  private final ObservableList<ReadyQueueRow> readyQueueRows = FXCollections.observableArrayList();
  private final TableView<ReadyQueueRow> readyQueueTable = new TableView<>(readyQueueRows);

  private final GanttChartPanel ganttPanelRR = new GanttChartPanel("Round Robin Gantt Chart");
  private final GanttChartPanel ganttPanelSJF = new GanttChartPanel("SJF Gantt Chart");
  private final GanttChartPanel ganttPanelSRTF = new GanttChartPanel("SRTF Gantt Chart");
  private final ResultsTable resultsTableRR = new ResultsTable("Results Table - Round Robin");
  private final ResultsTable resultsTableSJF = new ResultsTable("Results Table - SJF");
  private final ResultsTable resultsTableSRTF = new ResultsTable("Results Table - SRTF");

  private final TextArea finalConclusionArea = new TextArea();
  private final TextArea finalAnalysisArea = new TextArea();
  private final Button generateConclusionBtn = new Button("Generate Required Conclusion");
  private final Button generateAnalysisBtn = new Button("Generate Required Analysis");
  private final GridPane comparisonGrid = new GridPane();
  private final Map<String, ScheduleResult> lastResultsByAlgorithm = new HashMap<>();
  /** Wall-clock time of the last successful Run Simulation (for the log file). */
  private LocalDateTime lastSuccessfulSimulationAt;
  /** Quantum passed to the last successful Run Simulation. */
  private int lastSuccessfulRunQuantum = -1;

  private static final DateTimeFormatter SIM_LOG_TS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  /** True when the last successful run matched Scenario A (Basic mixed workload: test-cases/scenario-A). */
  private boolean lastWorkloadIsScenarioA;
  /** True when the last successful run matched Scenario B (Short-job-heavy case). */
  private boolean lastWorkloadIsScenarioB;
  /** True when the last successful run matched Scenario C (Fairness case: test-cases/scenario-C). */
  private boolean lastWorkloadIsScenarioC;
  /** True when the last successful run matched Scenario D (Long-job sensitivity: test-cases/scenario-D). */
  private boolean lastWorkloadIsLongJobSensitivityD;

  private final RoundRobinScheduler rrScheduler = new RoundRobinScheduler();
  private final SJFScheduler sjfScheduler = new SJFScheduler();
  private final SRTFScheduler srtfScheduler = new SRTFScheduler();

  private final Parent root;

  public MainController() {
    this.root = buildView();
  }

  public Parent getView() {
    return root;
  }

  public Parent buildView() {
    BorderPane rootPane = new BorderPane();
    rootPane.setStyle("-fx-background-color: #f0f0f0;");

    Label appTitle = new Label("CPU Scheduling Comparator — Round Robin / SJF / SRTF");
    appTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    HBox titleBar = new HBox(appTitle);
    titleBar.setAlignment(Pos.CENTER_LEFT);
    titleBar.setPadding(new Insets(10, 14, 10, 14));
    titleBar.setStyle(
        "-fx-background-color: #e6e6e6; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
    rootPane.setTop(titleBar);

    VBox left = new VBox(12);
    left.setPadding(new Insets(14));
    left.setFillWidth(true);
    left.setMaxWidth(Double.MAX_VALUE);
    left.setStyle(
        "-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #d0d0d0; "
            + "-fx-border-radius: 10; -fx-border-width: 1;");

    left.getChildren().add(buildInputPanel());
    left.getChildren().add(buildProcessTablePanel());
    left.getChildren().add(buildActionButtonsPanel());

    wireSimulationInputsLogListeners();

    // Right side sections in vertical order (better readability)
    SplitPane ganttSplit = new SplitPane();
    ganttSplit.setOrientation(Orientation.HORIZONTAL);
    ganttSplit.setDividerPositions(0.33, 0.66);
    ganttSplit.getItems().addAll(ganttPanelRR, ganttPanelSJF, ganttPanelSRTF);
    ganttSplit.setPrefHeight(320);
    for (GanttChartPanel p : List.of(ganttPanelRR, ganttPanelSJF, ganttPanelSRTF)) {
      p.setMinWidth(300);
    }

    VBox rrQueueBox = new VBox(8);
    configureReadyQueueTable();
    readyQueueTable.setMinHeight(320);
    readyQueueTable.setPrefHeight(320);
    rrQueueBox
        .getChildren()
        .addAll(wrapSectionTitle("RR ready queue (tick timeline)"), readyQueueTable);

    SplitPane resultsSplit = new SplitPane();
    resultsSplit.setOrientation(Orientation.HORIZONTAL);
    resultsSplit.setDividerPositions(0.33, 0.66);
    resultsSplit.getItems().addAll(resultsTableRR, resultsTableSJF, resultsTableSRTF);
    resultsSplit.setPrefHeight(260);

    VBox comparisonCard = new VBox(8);
    comparisonCard
        .getChildren()
        .addAll(comparisonSummarySectionTitle(), buildComparisonSummaryPanel());
    comparisonCard.setStyle(
        "-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #d0d0d0; "
            + "-fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 12;");

    VBox rightContent =
        new VBox(
            14,
            rrQueueBox,
            ganttSplit,
            resultsSplit,
            comparisonCard,
            buildConclusionAndAnalysisPanel());
    rightContent.setPadding(new Insets(8));
    rightContent.setFillWidth(true);

    // Full-width column: inputs on top, then all visualizations below (scroll top → bottom).
    VBox simulationColumn = new VBox(16, left, rightContent);
    simulationColumn.setPadding(new Insets(8, 12, 12, 12));
    simulationColumn.setFillWidth(true);

    ScrollPane simulationScroll = new ScrollPane(simulationColumn);
    simulationScroll.setFitToWidth(true);
    simulationScroll.setFitToHeight(false);
    simulationScroll.setPannable(true);
    simulationScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    simulationScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    simulationScroll.setStyle("-fx-background: #f0f0f0; -fx-background-color: #f0f0f0;");

    BorderPane.setMargin(simulationScroll, Insets.EMPTY);
    rootPane.setCenter(simulationScroll);

    finalConclusionArea.setEditable(false);
    finalConclusionArea.setWrapText(true);
    finalAnalysisArea.setEditable(false);
    finalAnalysisArea.setWrapText(true);

    return rootPane;
  }

  private static Label wrapSectionTitle(String text) {
    Label l = new Label(text);
    l.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333333;");
    return l;
  }

  /** Section heading styled like the reference report (white bar, rounded). */
  private static Label comparisonSummarySectionTitle() {
    Label l = new Label("Comparison summary");
    l.setMaxWidth(Double.MAX_VALUE);
    l.setStyle(
        "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333333; "
            + "-fx-background-color: #ffffff; -fx-padding: 10 12; -fx-background-radius: 8; "
            + "-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1;");
    return l;
  }

  private GridPane buildComparisonSummaryPanel() {
    comparisonGrid.getChildren().clear();
    comparisonGrid.getColumnConstraints().clear();
    comparisonGrid.setHgap(0);
    comparisonGrid.setVgap(0);
    comparisonGrid.setPadding(new Insets(4, 0, 0, 0));
    comparisonGrid.setStyle(
        "-fx-background-color: #ffffff; -fx-border-color: #d5d5d5; -fx-border-radius: 8; "
            + "-fx-border-width: 1;");

    String header =
        "-fx-background-color: #1a237e; -fx-text-fill: white; -fx-font-weight: bold; "
            + "-fx-padding: 10 14; -fx-alignment: center;";
    String metricName =
        "-fx-padding: 8 14; -fx-border-color: #ececec; -fx-border-width: 0 1 1 0; "
            + "-fx-background-color: #fafafa; -fx-font-weight: bold; -fx-text-fill: #37474f; "
            + "-fx-alignment: center-left;";
    String valueCell =
        "-fx-padding: 8 14; -fx-border-color: #ececec; -fx-border-width: 0 1 1 0; "
            + "-fx-background-color: #ffffff; -fx-alignment: center-right;";
    String winnerPlaceholder =
        "-fx-padding: 8 14; -fx-border-color: #ececec; -fx-border-width: 0 0 1 0; "
            + "-fx-background-color: #f5f5f5; -fx-alignment: center; -fx-text-fill: #757575;";

    comparisonGrid.add(comparisonTableHeaderLabel("Metric", header), 0, 0);
    comparisonGrid.add(comparisonTableHeaderLabel("Round Robin", header), 1, 0);
    comparisonGrid.add(comparisonTableHeaderLabel("SJF", header), 2, 0);
    comparisonGrid.add(comparisonTableHeaderLabel("SRTF", header), 3, 0);
    comparisonGrid.add(comparisonTableHeaderLabel("Winner", header), 4, 0);

    comparisonGrid.add(comparisonTableBodyLabel("Average WT", metricName), 0, 1);
    comparisonGrid.add(comparisonTableBodyLabel("-", valueCell), 1, 1);
    comparisonGrid.add(comparisonTableBodyLabel("-", valueCell), 2, 1);
    comparisonGrid.add(comparisonTableBodyLabel("-", valueCell), 3, 1);
    comparisonGrid.add(comparisonTableBodyLabel("—", winnerPlaceholder), 4, 1);

    comparisonGrid.add(comparisonTableBodyLabel("Average TAT", metricName), 0, 2);
    comparisonGrid.add(comparisonTableBodyLabel("-", valueCell), 1, 2);
    comparisonGrid.add(comparisonTableBodyLabel("-", valueCell), 2, 2);
    comparisonGrid.add(comparisonTableBodyLabel("-", valueCell), 3, 2);
    comparisonGrid.add(comparisonTableBodyLabel("—", winnerPlaceholder), 4, 2);

    comparisonGrid.add(comparisonTableBodyLabel("Average RT", metricName), 0, 3);
    comparisonGrid.add(comparisonTableBodyLabel("-", valueCell), 1, 3);
    comparisonGrid.add(comparisonTableBodyLabel("-", valueCell), 2, 3);
    comparisonGrid.add(comparisonTableBodyLabel("-", valueCell), 3, 3);
    comparisonGrid.add(comparisonTableBodyLabel("—", winnerPlaceholder), 4, 3);

    for (int i = 0; i < 5; i++) {
      ColumnConstraints cc = new ColumnConstraints();
      cc.setHgrow(Priority.SOMETIMES);
      cc.setMinWidth(i == 0 ? 108 : (i == 4 ? 160 : 92));
      comparisonGrid.getColumnConstraints().add(cc);
    }

    return comparisonGrid;
  }

  private static Label comparisonTableHeaderLabel(String text, String style) {
    Label l = new Label(text);
    l.setMaxWidth(Double.MAX_VALUE);
    l.setStyle(style);
    GridPane.setHgrow(l, Priority.ALWAYS);
    return l;
  }

  private static Label comparisonTableBodyLabel(String text, String style) {
    Label l = new Label(text);
    l.setMaxWidth(Double.MAX_VALUE);
    l.setStyle(style);
    GridPane.setHgrow(l, Priority.ALWAYS);
    return l;
  }

  /** Highlights winner cell: RR ≈ green, SJF ≈ yellow, SRTF ≈ blue; ties ≈ pale amber. */
  private static void applyWinnerCellStyle(Label label, String winnerLine) {
    label.setText(winnerLine);
    String base =
        "-fx-font-weight: bold; -fx-padding: 8 14; -fx-border-color: #ececec; -fx-border-width: 0 0 1 0; "
            + "-fx-alignment: center;";
    if ("All algorithms".equals(winnerLine)) {
      label.setStyle(base + " -fx-background-color: #eceff1; -fx-text-fill: #37474f;");
    } else if (winnerLine.contains(" and ")) {
      label.setStyle(base + " -fx-background-color: #fff9c4; -fx-text-fill: #5d4037;");
    } else if (winnerLine.startsWith("Round Robin")) {
      label.setStyle(base + " -fx-background-color: #c8e6c9; -fx-text-fill: #1b5e20;");
    } else if (winnerLine.startsWith("SJF")) {
      label.setStyle(base + " -fx-background-color: #fff9c4; -fx-text-fill: #f57f17;");
    } else if (winnerLine.startsWith("SRTF")) {
      label.setStyle(base + " -fx-background-color: #bbdefb; -fx-text-fill: #0d47a1;");
    } else {
      label.setStyle(base + " -fx-background-color: #f5f5f5; -fx-text-fill: #424242;");
    }
  }

  private void setWinnerAtGridRow(int row, String winnerText) {
    for (Node node : comparisonGrid.getChildren()) {
      Integer c = GridPane.getColumnIndex(node);
      Integer r = GridPane.getRowIndex(node);
      if (c != null && r != null && c == 4 && r == row && node instanceof Label label) {
        applyWinnerCellStyle(label, winnerText);
        return;
      }
    }
  }

  private VBox buildConclusionAndAnalysisPanel() {
    Label conclusionTitle = new Label("Final Conclusion");
    conclusionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
    finalConclusionArea.setPrefRowCount(9);
    finalConclusionArea.setPrefHeight(280);
    finalConclusionArea.setText("Run simulation, then press the button to generate required conclusion.");
    generateConclusionBtn.setOnAction(e -> onGenerateRequiredConclusion());
    generateConclusionBtn.setDisable(true);
    VBox.setVgrow(finalConclusionArea, Priority.ALWAYS);
    VBox conclusionBox = new VBox(8, conclusionTitle, finalConclusionArea, generateConclusionBtn);
    conclusionBox.setPadding(new Insets(12));
    conclusionBox.setStyle(
        "-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #d0d0d0; "
            + "-fx-border-radius: 10; -fx-border-width: 1;");

    Label analysisTitle = new Label("Final Analysis");
    analysisTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
    finalAnalysisArea.setPrefRowCount(10);
    finalAnalysisArea.setPrefHeight(320);
    finalAnalysisArea.setText("Run simulation, then press the button to generate required analysis.");
    generateAnalysisBtn.setOnAction(e -> onGenerateRequiredAnalysis());
    generateAnalysisBtn.setDisable(true);
    VBox.setVgrow(finalAnalysisArea, Priority.ALWAYS);
    VBox analysisBox = new VBox(8, analysisTitle, finalAnalysisArea, generateAnalysisBtn);
    analysisBox.setPadding(new Insets(12));
    analysisBox.setStyle(
        "-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #d0d0d0; "
            + "-fx-border-radius: 10; -fx-border-width: 1;");

    return new VBox(12, conclusionBox, analysisBox);
  }

  private VBox buildInputPanel() {
    Label title = new Label("Input Panel");
    title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

    GridPane form = new GridPane();
    form.setHgap(10);
    form.setVgap(10);

    processIdField.setPromptText("e.g., P1");
    arrivalTimeField.setPromptText("e.g., 0");
    burstTimeField.setPromptText("e.g., 6");
    timeQuantumField.setPromptText("e.g., 3");

    form.add(new Label("Process ID:"), 0, 0);
    form.add(processIdField, 1, 0);

    form.add(new Label("Arrival Time:"), 0, 1);
    form.add(arrivalTimeField, 1, 1);

    form.add(new Label("Burst Time:"), 0, 2);
    form.add(burstTimeField, 1, 2);

    form.add(new Label("Time Quantum (RR):"), 0, 3);
    form.add(timeQuantumField, 1, 3);

    scenarioCombo.getItems().setAll(
        "Scenario A - Basic mixed workload",
        "Scenario B - Short-job-heavy case",
        "Scenario C - Fairness case",
        "Scenario D - Long-job sensitivity case",
        "Scenario E - Validation case"
    );
    scenarioCombo.setPromptText("Choose scenario...");

    Button loadScenarioBtn = new Button("Load Scenario");
    loadScenarioBtn.setOnAction(e -> applySelectedScenario());

    form.add(new Label("Preset Scenario:"), 0, 4);
    form.add(scenarioCombo, 1, 4);
    form.add(loadScenarioBtn, 1, 5);

    VBox box = new VBox(10, title, form);
    box.setPadding(new Insets(8));
    box.setMaxWidth(350);
    return box;
  }

  private void     applySelectedScenario() {
    String selected = scenarioCombo.getValue();
    if (selected == null || selected.isBlank()) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Scenario Selection");
      alert.setHeaderText("No scenario selected");
      alert.setContentText("Please choose a scenario first.");
      alert.showAndWait();
      return;
    }

    clearWorkspaceAndOutputs();

    if (selected.startsWith("Scenario A")) {
      // Basic mixed workload
      processRows.add(new ProcessRow("P1", "0", "6"));
      processRows.add(new ProcessRow("P2", "1", "4"));
      processRows.add(new ProcessRow("P3", "2", "2"));
      processRows.add(new ProcessRow("P4", "3", "5"));
      timeQuantumField.setText("3");
      return;
    }

    if (selected.startsWith("Scenario B")) {
      // Short-job-heavy case
      processRows.add(new ProcessRow("P1", "0", "1"));
      processRows.add(new ProcessRow("P2", "0", "2"));
      processRows.add(new ProcessRow("P3", "1", "1"));
      processRows.add(new ProcessRow("P4", "2", "3"));
      processRows.add(new ProcessRow("P5", "3", "1"));
      timeQuantumField.setText("2");
      return;
    }

    if (selected.startsWith("Scenario C")) {
      // Fairness case: equal bursts, same arrival — RR rotates slices across all processes (see test-cases/scenario-C.txt).
      processRows.add(new ProcessRow("P1", "0", "8"));
      processRows.add(new ProcessRow("P2", "0", "8"));
      processRows.add(new ProcessRow("P3", "0", "8"));
      timeQuantumField.setText("2");
      return;
    }

    if (selected.startsWith("Scenario D")) {
      // Long-job sensitivity: one long process vs shorter jobs; RR vs SJF vs SRTF differ (see test-cases/scenario-D.txt).
      processRows.add(new ProcessRow("P1", "0", "20"));
      processRows.add(new ProcessRow("P2", "1", "2"));
      processRows.add(new ProcessRow("P3", "2", "3"));
      processRows.add(new ProcessRow("P4", "3", "1"));
      timeQuantumField.setText("4");
      return;
    }

    if (selected.startsWith("Scenario E")) {
      // Validation case with intentionally invalid inputs.
      processRows.add(new ProcessRow("", "0", "5"));      // invalid: missing process id
      processRows.add(new ProcessRow("P2", "-1", "3"));   // invalid: negative arrival
      processRows.add(new ProcessRow("P3", "2", "0"));    // invalid: non-positive burst
      timeQuantumField.setText("0");                      // invalid: non-positive quantum
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Scenario E Loaded");
      alert.setHeaderText("Validation case is ready");
      alert.setContentText("Press Run Simulation to see validation behavior.");
      alert.showAndWait();
    }
  }

  private VBox buildProcessTablePanel() {
    Label title = new Label("Process input table");
    title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

    processTable.setEditable(true);
    processTable.setStyle("-fx-border-color: #d0d0d0; -fx-background-radius: 6;");

    TableColumn<ProcessRow, String> idCol = new TableColumn<>("Process ID");
    idCol.setCellValueFactory(cell -> cell.getValue().processIdProperty());
    idCol.setCellFactory(TextFieldTableCell.forTableColumn());
    idCol.setPrefWidth(120);

    TableColumn<ProcessRow, String> arrivalCol = new TableColumn<>("Arrival Time");
    arrivalCol.setCellValueFactory(cell -> cell.getValue().arrivalTimeProperty());
    arrivalCol.setCellFactory(TextFieldTableCell.forTableColumn());
    arrivalCol.setPrefWidth(120);

    TableColumn<ProcessRow, String> burstCol = new TableColumn<>("Burst Time");
    burstCol.setCellValueFactory(cell -> cell.getValue().burstTimeProperty());
    burstCol.setCellFactory(TextFieldTableCell.forTableColumn());
    burstCol.setPrefWidth(120);

    processTable.getColumns().addAll(idCol, arrivalCol, burstCol);
    processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    attachProcessTableDynamicHeight();

    VBox box = new VBox(8, title, processTable);
    box.setPadding(new Insets(8));
    box.setMaxWidth(Double.MAX_VALUE);
    return box;
  }

  /**
   * Keeps the process input table height in sync with {@link #processRows}: more rows ⇒ taller
   * table (like a spreadsheet), up to {@link #PROCESS_TABLE_MAX_VIEW_HEIGHT} then internal scroll.
   */
  private void attachProcessTableDynamicHeight() {
    processTable.setFixedCellSize(PROCESS_TABLE_ROW_HEIGHT);
    InvalidationListener syncHeight =
        o -> {
          int n = processRows.size();
          int bodyRows = Math.max(PROCESS_TABLE_MIN_VISIBLE_ROWS, n);
          double h =
              PROCESS_TABLE_HEADER_HEIGHT + bodyRows * PROCESS_TABLE_ROW_HEIGHT + 10;
          h = Math.min(h, PROCESS_TABLE_MAX_VIEW_HEIGHT);
          processTable.setMinHeight(h);
          processTable.setPrefHeight(h);
          processTable.setMaxHeight(h);
        };
    processRows.addListener((ListChangeListener<? super ProcessRow>) c -> syncHeight.invalidated(null));
    syncHeight.invalidated(null);
  }

  private void configureReadyQueueTable() {
    readyQueueTable.setEditable(false);
    readyQueueTable.setStyle("-fx-border-color: #d0d0d0; -fx-background-radius: 6;");
    Label ph = new Label("Run simulation to show Round Robin queue state (CPU + waiting line).");
    ph.setStyle("-fx-text-fill: #607d8b;");
    readyQueueTable.setPlaceholder(ph);
    TableColumn<ReadyQueueRow, String> tickCol = new TableColumn<>("Time");
    tickCol.setCellValueFactory(cell -> cell.getValue().timeTickProperty());
    tickCol.setPrefWidth(72);

    TableColumn<ReadyQueueRow, String> queueCol = new TableColumn<>("CPU → Ready queue (in order)");
    queueCol.setCellValueFactory(
        cell -> {
          ReadyQueueRow r = cell.getValue();
          return new ReadOnlyStringWrapper(r == null ? "" : r.getQueueSummaryText());
        });
    queueCol.setPrefWidth(720);
    queueCol.setCellFactory(
        col ->
            new TableCell<ReadyQueueRow, String>() {
              @Override
              protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (empty || getTableRow() == null) {
                  setGraphic(null);
                  setText(null);
                  return;
                }
                ReadyQueueRow row = getTableRow().getItem();
                if (row == null) {
                  setGraphic(null);
                  return;
                }
                setText(null);
                setGraphic(buildQueueStateGraphic(row.getQueueOrder()));
              }
            });

    readyQueueTable.getColumns().setAll(tickCol, queueCol);
    readyQueueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
  }

  /** Visual row: left = process on CPU, then arrows to waiting processes (same colors as Gantt). */
  private static HBox buildQueueStateGraphic(List<String> order) {
    HBox box = new HBox(6);
    box.setAlignment(Pos.CENTER_LEFT);

    if (order.isEmpty()) {
      Label idle = new Label("CPU idle · ready queue empty");
      idle.setStyle("-fx-text-fill: #78909c; -fx-font-style: italic;");
      box.getChildren().add(idle);
      return box;
    }

    for (int i = 0; i < order.size(); i++) {
      if (i > 0) {
        Label arrow = new Label("→");
        arrow.setStyle("-fx-text-fill: #90a4ae; -fx-font-weight: bold;");
        box.getChildren().add(arrow);
      }
      String pid = order.get(i);
      Color fill = ProcessColorPalette.fillForBlock(pid);
      Color fg = ProcessColorPalette.textColorForGanttFill(fill);
      Label chip = new Label(pid);
      String border =
          i == 0
              ? "-fx-border-width: 2; -fx-border-color: #37474f;"
              : "-fx-border-width: 1; -fx-border-color: #546e7a;";
      chip.setStyle(
          "-fx-background-color: "
              + ProcessColorPalette.cssRgb(fill)
              + "; -fx-text-fill: "
              + ProcessColorPalette.cssRgb(fg)
              + "; -fx-padding: 4 10; -fx-background-radius: 6; -fx-font-weight: bold; "
              + border);
      box.getChildren().add(chip);
      if (i == 0) {
        Label tag = new Label("on CPU");
        tag.setStyle("-fx-font-size: 10px; -fx-text-fill: #455a64;");
        box.getChildren().add(tag);
      }
    }
    return box;
  }

  private HBox buildActionButtonsPanel() {
    Button addBtn = new Button("Add Process");
    Button removeBtn = new Button("Remove Selected");
    Button runBtn = new Button("Run Simulation");
    Button resetBtn = new Button("Reset");
    Button clearBtn = new Button("Clear All");

    addBtn.setOnAction(e -> addProcessFromForm());
    removeBtn.setOnAction(e -> removeSelected());
    resetBtn.setOnAction(e -> resetToFreshGui());
    clearBtn.setOnAction(e -> clearWorkspaceAndOutputs());
    runBtn.setOnAction(e -> runSimulation());

    String base = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 9 14; -fx-background-radius: 6;";
    String secondary = base + "-fx-background-color: #eceff1; -fx-text-fill: #37474f; -fx-border-color: #b0bec5; -fx-border-radius: 6;";
    addBtn.setStyle(secondary);
    removeBtn.setStyle(secondary);
    resetBtn.setStyle(secondary);
    runBtn.setStyle(
        base
            + "-fx-background-color: #1976d2; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 1);");
    clearBtn.setStyle(base + "-fx-background-color: #c62828; -fx-text-fill: white;");

    addBtn.setMinWidth(150);
    removeBtn.setMinWidth(170);
    resetBtn.setMinWidth(110);
    runBtn.setMinWidth(170);
    clearBtn.setMinWidth(130);

    HBox box = new HBox(12, addBtn, removeBtn, runBtn, resetBtn, clearBtn);
    box.setAlignment(Pos.CENTER_LEFT);
    box.setPadding(new Insets(8));
    return box;
  }

  /**
   * Keeps {@code simulation-inputs.log} in sync with the full input UI: form fields, scenario
   * combo, every table row, and the last successful run block.
   */
  private void wireSimulationInputsLogListeners() {
    Runnable sync = this::syncSimulationInputsLog;
    processRows.addListener(
        (ListChangeListener<? super ProcessRow>) c -> {
          while (c.next()) {
            if (c.wasAdded()) {
              for (ProcessRow row : c.getAddedSubList()) {
                attachProcessRowLogListeners(row);
              }
            }
          }
          sync.run();
        });
    timeQuantumField.textProperty().addListener((o, a, b) -> sync.run());
    processIdField.textProperty().addListener((o, a, b) -> sync.run());
    arrivalTimeField.textProperty().addListener((o, a, b) -> sync.run());
    burstTimeField.textProperty().addListener((o, a, b) -> sync.run());
    scenarioCombo
        .valueProperty()
        .addListener(
            (o, a, b) -> sync.run());
    sync.run();
  }

  private void attachProcessRowLogListeners(ProcessRow row) {
    Runnable sync = this::syncSimulationInputsLog;
    row.processIdProperty().addListener((o, x, y) -> sync.run());
    row.arrivalTimeProperty().addListener((o, x, y) -> sync.run());
    row.burstTimeProperty().addListener((o, x, y) -> sync.run());
  }

  private void syncSimulationInputsLog() {
    List<InputLogWriter.TableRowLine> lines = new ArrayList<>();
    for (ProcessRow r : processRows) {
      lines.add(
          new InputLogWriter.TableRowLine(
              r.getProcessId(),
              r.getArrivalTime(),
              r.getBurstTime(),
              r.isManualFromForm()));
    }
    String scenario = scenarioCombo.getValue();
    String scenarioDisplay =
        scenario == null || scenario.isBlank() ? "(none selected)" : scenario;
    InputLogWriter.rewriteSimulationInputsLog(
        processIdField.getText(),
        arrivalTimeField.getText(),
        burstTimeField.getText(),
        timeQuantumField.getText(),
        scenarioDisplay,
        lines,
        buildLastSuccessfulSimulationSection());
  }

  private String buildLastSuccessfulSimulationSection() {
    if (lastResultsByAlgorithm.isEmpty() || lastSuccessfulRunQuantum < 0) {
      return null;
    }
    ScheduleResult rr = lastResultsByAlgorithm.get("Round Robin");
    ScheduleResult sjf = lastResultsByAlgorithm.get("SJF");
    ScheduleResult srtf = lastResultsByAlgorithm.get("SRTF");
    if (rr == null || sjf == null || srtf == null) {
      return null;
    }
    String ls = System.lineSeparator();
    StringBuilder sb = new StringBuilder();
    if (lastSuccessfulSimulationAt != null) {
      sb.append("Completed at: ")
          .append(SIM_LOG_TS.format(lastSuccessfulSimulationAt))
          .append(ls);
    }
    sb.append("Time Quantum used: ").append(lastSuccessfulRunQuantum).append(ls);
    sb.append(
        String.format(
            Locale.US,
            "Average WT — RR: %.2f | SJF: %.2f | SRTF: %.2f",
            rr.getAverageWaitingTime(),
            sjf.getAverageWaitingTime(),
            srtf.getAverageWaitingTime()));
    sb.append(ls);
    sb.append(
        String.format(
            Locale.US,
            "Average TAT — RR: %.2f | SJF: %.2f | SRTF: %.2f",
            rr.getAverageTurnaroundTime(),
            sjf.getAverageTurnaroundTime(),
            srtf.getAverageTurnaroundTime()));
    sb.append(ls);
    sb.append(
        String.format(
            Locale.US,
            "Average RT — RR: %.2f | SJF: %.2f | SRTF: %.2f",
            rr.getAverageResponseTime(),
            sjf.getAverageResponseTime(),
            srtf.getAverageResponseTime()));
    return sb.toString();
  }

  private void addProcessFromForm() {
    Set<String> existingIds = new HashSet<>();
    for (ProcessRow row : processRows) {
      String id = row.getProcessId();
      if (id != null) {
        String t = id.trim();
        if (!t.isEmpty()) {
          existingIds.add(t);
        }
      }
    }

    List<String> errors =
        InputValidator.validateAddProcessForm(
            processIdField.getText(),
            arrivalTimeField.getText(),
            burstTimeField.getText(),
            timeQuantumField.getText(),
            existingIds);

    if (!errors.isEmpty()) {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle("Invalid input");
      alert.setHeaderText("Cannot add this process.");
      alert.setContentText(String.join("\n", errors));
      alert.showAndWait();
      return;
    }

    ProcessRow row =
        new ProcessRow(
            processIdField.getText().trim(),
            arrivalTimeField.getText().trim(),
            burstTimeField.getText().trim(),
            true);
    processRows.add(row);
  }

  /**
   * Full reset: empty GUI state (inputs + outputs + preset selection). {@code simulation-inputs.log}
   * is rewritten to match (via {@link #syncSimulationInputsLog()}).
   */
  private void resetToFreshGui() {
    clearWorkspaceAndOutputs();
    processIdField.clear();
    arrivalTimeField.clear();
    burstTimeField.clear();
    timeQuantumField.clear();
    scenarioCombo.getSelectionModel().clearSelection();
    syncSimulationInputsLog();
  }

  private void removeSelected() {
    ProcessRow selected = processTable.getSelectionModel().getSelectedItem();
    if (selected != null) {
      processRows.remove(selected);
    }
  }

  /** Clears simulation outputs and the process table; {@link #syncSimulationInputsLog} reflects the wipe. */
  private void clearWorkspaceAndOutputs() {
    readyQueueRows.clear();
    resultsTableRR.setScheduleResult(null);
    resultsTableSJF.setScheduleResult(null);
    resultsTableSRTF.setScheduleResult(null);
    ganttPanelRR.setScheduleResult(null);
    ganttPanelSJF.setScheduleResult(null);
    ganttPanelSRTF.setScheduleResult(null);
    lastResultsByAlgorithm.clear();
    lastSuccessfulSimulationAt = null;
    lastSuccessfulRunQuantum = -1;
    lastWorkloadIsScenarioA = false;
    lastWorkloadIsScenarioB = false;
    lastWorkloadIsScenarioC = false;
    lastWorkloadIsLongJobSensitivityD = false;
    generateConclusionBtn.setDisable(true);
    generateAnalysisBtn.setDisable(true);
    buildComparisonSummaryPanel();
    finalConclusionArea.setText("Run simulation, then press the button to generate required conclusion.");
    finalAnalysisArea.setText("Run simulation, then press the button to generate required analysis.");
    processRows.clear();
    syncSimulationInputsLog();
  }

  private void runSimulation() {
    try {
      List<ProcessDraft> drafts = new ArrayList<>();
      for (ProcessRow row : processRows) {
        drafts.add(new ProcessDraft(row.getProcessId(), row.getArrivalTime(), row.getBurstTime()));
      }

      InputValidator.ValidationResult validation = InputValidator.validateForSimulation(
          drafts,
          timeQuantumField.getText()
      );

      if (!validation.isValid()) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Input Validation Error");
        alert.setHeaderText("Fix the following issues:");
        alert.setContentText(String.join("\n", validation.getErrors()));
        alert.showAndWait();
        return;
      }

      int quantum = validation.getTimeQuantum();
      List<Process> processes = validation.getProcesses();
      lastWorkloadIsScenarioA = matchesBasicMixedWorkloadScenarioA(processes, quantum);
      lastWorkloadIsScenarioB = matchesShortJobHeavyScenarioB(processes, quantum);
      lastWorkloadIsScenarioC = matchesFairnessScenarioC(processes, quantum);
      lastWorkloadIsLongJobSensitivityD = matchesLongJobSensitivityScenarioD(processes, quantum);

      ScheduleResult rr = rrScheduler.schedule(processes, quantum);
      ScheduleResult sjf = sjfScheduler.schedule(processes, quantum);
      ScheduleResult srtf = srtfScheduler.schedule(processes, quantum);
      lastResultsByAlgorithm.clear();
      lastResultsByAlgorithm.put("Round Robin", rr);
      lastResultsByAlgorithm.put("SJF", sjf);
      lastResultsByAlgorithm.put("SRTF", srtf);
      generateConclusionBtn.setDisable(false);
      generateAnalysisBtn.setDisable(false);

      lastSuccessfulSimulationAt = LocalDateTime.now();
      lastSuccessfulRunQuantum = quantum;

      ganttPanelRR.setScheduleResult(rr);
      ganttPanelSJF.setScheduleResult(sjf);
      ganttPanelSRTF.setScheduleResult(srtf);

      // RR ready queue view
      readyQueueRows.clear();
      for (ScheduleResult.ReadyQueueSnapshot snap : rr.getReadyQueueSnapshots()) {
        readyQueueRows.add(
            new ReadyQueueRow("t = " + snap.getTimeTick(), snap.getQueueProcessIds()));
      }

      resultsTableRR.setScheduleResult(rr);
      resultsTableSJF.setScheduleResult(sjf);
      resultsTableSRTF.setScheduleResult(srtf);

      updateComparison(rr, sjf, srtf);
      finalConclusionArea.setText("Simulation completed. Press \"Generate Required Conclusion\".");
      finalAnalysisArea.setText("Simulation completed. Press \"Generate Required Analysis\".");

      syncSimulationInputsLog();

    } catch (Exception ex) {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle("Simulation Error");
      alert.setHeaderText("The simulation could not be completed.");
      alert.setContentText(ex.getMessage());
      alert.showAndWait();
    }
  }

  private void updateComparison(ScheduleResult rr, ScheduleResult sjf, ScheduleResult srtf) {
    // Grid: col 1–3 = RR, SJF, SRTF; col 4 = Winner; rows 1–3 = WT, TAT, RT.
    setGridValue(1, 1, format2(rr.getAverageWaitingTime()));
    setGridValue(2, 1, format2(sjf.getAverageWaitingTime()));
    setGridValue(3, 1, format2(srtf.getAverageWaitingTime()));
    setGridValue(1, 2, format2(rr.getAverageTurnaroundTime()));
    setGridValue(2, 2, format2(sjf.getAverageTurnaroundTime()));
    setGridValue(3, 2, format2(srtf.getAverageTurnaroundTime()));
    setGridValue(1, 3, format2(rr.getAverageResponseTime()));
    setGridValue(2, 3, format2(sjf.getAverageResponseTime()));
    setGridValue(3, 3, format2(srtf.getAverageResponseTime()));

    String wtWin =
        minName(rr.getAverageWaitingTime(), "Round Robin", sjf.getAverageWaitingTime(), "SJF", srtf.getAverageWaitingTime(), "SRTF");
    String tatWin =
        minName(rr.getAverageTurnaroundTime(), "Round Robin", sjf.getAverageTurnaroundTime(), "SJF", srtf.getAverageTurnaroundTime(), "SRTF");
    String rtWin =
        minName(rr.getAverageResponseTime(), "Round Robin", sjf.getAverageResponseTime(), "SJF", srtf.getAverageResponseTime(), "SRTF");
    setWinnerAtGridRow(1, wtWin);
    setWinnerAtGridRow(2, tatWin);
    setWinnerAtGridRow(3, rtWin);

  }

  private void setGridValue(int col, int row, String value) {
    for (javafx.scene.Node node : comparisonGrid.getChildren()) {
      Integer c = GridPane.getColumnIndex(node);
      Integer r = GridPane.getRowIndex(node);
      if (c != null && r != null && c == col && r == row && node instanceof Label label) {
        label.setText(value);
        return;
      }
    }
  }

  private static String format2(double v) {
    return String.format(Locale.US, "%.2f", v);
  }

  private String generateConclusion(ScheduleResult rr, ScheduleResult sjf, ScheduleResult srtf) {
    double rrAvgWT = rr.getAverageWaitingTime();
    double sjfAvgWT = sjf.getAverageWaitingTime();
    double srtfAvgWT = srtf.getAverageWaitingTime();
    double rrAvgRT = rr.getAverageResponseTime();
    double sjfAvgRT = sjf.getAverageResponseTime();
    double srtfAvgRT = srtf.getAverageResponseTime();

    int rrSpread = wtSpread(rr);
    int sjfSpread = wtSpread(sjf);
    int srtfSpread = wtSpread(srtf);

    String wtWinner = minName(rrAvgWT, "Round Robin", sjfAvgWT, "SJF", srtfAvgWT, "SRTF");
    String rtWinner = minName(rrAvgRT, "Round Robin", sjfAvgRT, "SJF", srtfAvgRT, "SRTF");
    String fairnessWinner = minName(rrSpread, "Round Robin", sjfSpread, "SJF", srtfSpread, "SRTF");
    String recommended = chooseRecommendation(
        rrAvgWT, sjfAvgWT, srtfAvgWT,
        rrAvgRT, sjfAvgRT, srtfAvgRT,
        rrSpread, sjfSpread, srtfSpread
    );

    String reason;
    if ("Round Robin".equals(recommended)) {
      reason = "it balances fairness (waiting-time spread) with good average waiting and response times for this workload";
    } else if ("SRTF".equals(recommended)) {
      reason = "it dynamically preempts longer jobs and usually minimizes waiting and response times for mixed workloads";
    } else {
      reason = "it achieves better efficiency on this workload by reducing average waiting/response time despite its non-preemptive nature";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("Based on the simulation results:\n\n");
    sb.append("Average WT: RR=")
        .append(format2(rrAvgWT))
        .append(" vs SJF=")
        .append(format2(sjfAvgWT))
        .append(" vs SRTF=")
        .append(format2(srtfAvgWT))
        .append(" \u2192 ")
        .append(wtWinner)
        .append(" performed better\n");

    sb.append("Average RT: RR=")
        .append(format2(rrAvgRT))
        .append(" vs SJF=")
        .append(format2(sjfAvgRT))
        .append(" vs SRTF=")
        .append(format2(srtfAvgRT))
        .append(" \u2192 ")
        .append(rtWinner)
        .append(" had faster first response\n");

    sb.append("Fairness (max-min WT spread): RR=")
        .append(rrSpread)
        .append(" vs SJF=")
        .append(sjfSpread)
        .append(" vs SRTF=")
        .append(srtfSpread)
        .append(" \u2192 ")
        .append(fairnessWinner)
        .append(" was more fair\n");

    sb.append("Recommendation: ")
        .append(recommended)
        .append(" is recommended for this workload because ")
        .append(reason)
        .append(".\n");

    return sb.toString();
  }

  private void onGenerateRequiredConclusion() {
    if (lastResultsByAlgorithm.isEmpty()) {
      finalConclusionArea.setText("Please run simulation first.");
      return;
    }
    ScheduleResult rr = lastResultsByAlgorithm.get("Round Robin");
    ScheduleResult sjf = lastResultsByAlgorithm.get("SJF");
    ScheduleResult srtf = lastResultsByAlgorithm.get("SRTF");
    finalConclusionArea.setText(generateRequiredConclusion(rr, sjf, srtf));
  }

  private void onGenerateRequiredAnalysis() {
    if (lastResultsByAlgorithm.isEmpty()) {
      finalAnalysisArea.setText("Please run simulation first.");
      return;
    }
    ScheduleResult rr = lastResultsByAlgorithm.get("Round Robin");
    ScheduleResult sjf = lastResultsByAlgorithm.get("SJF");
    ScheduleResult srtf = lastResultsByAlgorithm.get("SRTF");
    finalAnalysisArea.setText(generateRequiredAnalysis(rr, sjf, srtf));
  }

  private String generateRequiredConclusion(ScheduleResult rr, ScheduleResult sjf, ScheduleResult srtf) {
    if (lastWorkloadIsScenarioA) {
      return REQUIRED_CONCLUSION_SCENARIO_A;
    }
    if (lastWorkloadIsScenarioB) {
      return REQUIRED_CONCLUSION_SCENARIO_B;
    }
    if (lastWorkloadIsScenarioC) {
      return REQUIRED_CONCLUSION_SCENARIO_C;
    }
    if (lastWorkloadIsLongJobSensitivityD) {
      return REQUIRED_CONCLUSION_SCENARIO_D;
    }

    String bestOverall = chooseRecommendation(
        rr.getAverageWaitingTime(), sjf.getAverageWaitingTime(), srtf.getAverageWaitingTime(),
        rr.getAverageResponseTime(), sjf.getAverageResponseTime(), srtf.getAverageResponseTime(),
        wtSpread(rr), wtSpread(sjf), wtSpread(srtf)
    );

    String bestWt = minName(rr.getAverageWaitingTime(), "Round Robin", sjf.getAverageWaitingTime(), "SJF", srtf.getAverageWaitingTime(), "SRTF");
    String bestTat = minName(rr.getAverageTurnaroundTime(), "Round Robin", sjf.getAverageTurnaroundTime(), "SJF", srtf.getAverageTurnaroundTime(), "SRTF");
    String bestRt = minName(rr.getAverageResponseTime(), "Round Robin", sjf.getAverageResponseTime(), "SJF", srtf.getAverageResponseTime(), "SRTF");
    String fairest = minName(wtSpread(rr), "Round Robin", wtSpread(sjf), "SJF", wtSpread(srtf), "SRTF");

    String tradeOff = "Efficiency prefers lower average waiting/turnaround/response times, while urgency prefers faster reaction to newly arrived or short jobs. "
        + "In this workload, SRTF usually improves urgency by preempting long CPU bursts, but Round Robin can still be preferred when balanced fairness is needed.";

    return "Required Conclusion:\n\n"
        + "- Better overall algorithm on this dataset: " + bestOverall + ".\n"
        + "- Metric winners: WT -> " + bestWt + ", TAT -> " + bestTat + ", RT -> " + bestRt + ".\n"
        + "- Trade-off between efficiency and urgency: " + tradeOff + "\n"
        + "- Fairer in practice (lower WT spread): " + fairest + ".";
  }

  private String generateRequiredAnalysis(ScheduleResult rr, ScheduleResult sjf, ScheduleResult srtf) {
    if (lastWorkloadIsScenarioA) {
      return REQUIRED_ANALYSIS_SCENARIO_A;
    }
    if (lastWorkloadIsScenarioB) {
      return REQUIRED_ANALYSIS_SCENARIO_B;
    }
    if (lastWorkloadIsScenarioC) {
      return REQUIRED_ANALYSIS_SCENARIO_C;
    }
    if (lastWorkloadIsLongJobSensitivityD) {
      return REQUIRED_ANALYSIS_SCENARIO_D;
    }

    String lowerWt = minName(rr.getAverageWaitingTime(), "Round Robin", sjf.getAverageWaitingTime(), "SJF", srtf.getAverageWaitingTime(), "SRTF");
    String lowerTat = minName(rr.getAverageTurnaroundTime(), "Round Robin", sjf.getAverageTurnaroundTime(), "SJF", srtf.getAverageTurnaroundTime(), "SRTF");
    boolean sjfShortJobs = sjf.getAverageWaitingTime() <= rr.getAverageWaitingTime() || sjf.getAverageWaitingTime() <= srtf.getAverageWaitingTime();

    int maxSpread = Math.max(wtSpread(rr), Math.max(wtSpread(sjf), wtSpread(srtf)));
    String starvationNote = maxSpread > 20
        ? "Noticeable unfair delay appeared for some processes (high waiting-time spread)."
        : "No severe starvation appeared in this run, but some delay differences still exist.";

    String recommendation = chooseRecommendation(
        rr.getAverageWaitingTime(), sjf.getAverageWaitingTime(), srtf.getAverageWaitingTime(),
        rr.getAverageResponseTime(), sjf.getAverageResponseTime(), srtf.getAverageResponseTime(),
        wtSpread(rr), wtSpread(sjf), wtSpread(srtf)
    );

    return "Required Analysis Questions:\n\n"
        + "1) Which algorithm gave lower average waiting time?\n"
        + "-> " + lowerWt + ".\n\n"
        + "2) Which algorithm gave lower average turnaround time?\n"
        + "-> " + lowerTat + ".\n\n"
        + "3) Did SJF favor short jobs more strongly?\n"
        + "-> " + (sjfShortJobs ? "Yes, SJF behavior indicates stronger short-job preference." : "Not strongly in this dataset.") + "\n\n"
        + "4) Did Priority Scheduling favor urgent processes more strongly?\n"
        + "-> Priority Scheduling is not implemented in this simulator run, so this cannot be measured directly.\n\n"
        + "5) Was any starvation or unfair delay observed?\n"
        + "-> " + starvationNote + "\n\n"
        + "6) Which algorithm would you recommend for the tested workload, and why?\n"
        + "-> " + recommendation + ", because it gave the best balance between efficiency metrics and fairness for this dataset.";
  }

  private static String minName(double v1, String n1, double v2, String n2, double v3, String n3) {
    double min = Math.min(v1, Math.min(v2, v3));
    boolean b1 = Math.abs(v1 - min) < 1e-9;
    boolean b2 = Math.abs(v2 - min) < 1e-9;
    boolean b3 = Math.abs(v3 - min) < 1e-9;
    if (b1 && b2 && b3) return "All algorithms";
    if (b1 && b2) return n1 + " and " + n2;
    if (b1 && b3) return n1 + " and " + n3;
    if (b2 && b3) return n2 + " and " + n3;
    return b1 ? n1 : (b2 ? n2 : n3);
  }

  private static String minName(int v1, String n1, int v2, String n2, int v3, String n3) {
    int min = Math.min(v1, Math.min(v2, v3));
    boolean b1 = v1 == min;
    boolean b2 = v2 == min;
    boolean b3 = v3 == min;
    if (b1 && b2 && b3) return "All algorithms";
    if (b1 && b2) return n1 + " and " + n2;
    if (b1 && b3) return n1 + " and " + n3;
    if (b2 && b3) return n2 + " and " + n3;
    return b1 ? n1 : (b2 ? n2 : n3);
  }

  private static String chooseRecommendation(
      double rrWt, double sjfWt, double srtfWt,
      double rrRt, double sjfRt, double srtfRt,
      int rrSpread, int sjfSpread, int srtfSpread
  ) {
    int minSpread = Math.min(rrSpread, Math.min(sjfSpread, srtfSpread));
    if (srtfSpread == minSpread) return "SRTF";
    if (rrSpread == minSpread && rrWt <= sjfWt && rrWt <= srtfWt) return "Round Robin";
    if (sjfSpread == minSpread && sjfWt <= rrWt && sjfWt <= srtfWt) return "SJF";

    double minWt = Math.min(rrWt, Math.min(sjfWt, srtfWt));
    if (Math.abs(srtfWt - minWt) < 1e-9) return "SRTF";
    if (Math.abs(rrWt - minWt) < 1e-9 && rrRt <= sjfRt && rrRt <= srtfRt) return "Round Robin";
    if (Math.abs(sjfWt - minWt) < 1e-9 && sjfRt <= rrRt && sjfRt <= srtfRt) return "SJF";

    double minRt = Math.min(rrRt, Math.min(sjfRt, srtfRt));
    if (Math.abs(srtfRt - minRt) < 1e-9) return "SRTF";
    if (Math.abs(rrRt - minRt) < 1e-9) return "Round Robin";
    return "SJF";
  }

  /** Same process list and quantum as preset “Scenario B - Short-job-heavy case”. */
  private static boolean matchesShortJobHeavyScenarioB(List<Process> processes, int quantum) {
    if (processes == null || processes.size() != 5 || quantum != 2) {
      return false;
    }
    Map<String, Process> byId = new HashMap<>();
    for (Process p : processes) {
      byId.put(p.getProcessId(), p);
    }
    Process p1 = byId.get("P1");
    Process p2 = byId.get("P2");
    Process p3 = byId.get("P3");
    Process p4 = byId.get("P4");
    Process p5 = byId.get("P5");
    if (p1 == null || p2 == null || p3 == null || p4 == null || p5 == null) {
      return false;
    }
    return p1.getArrivalTime() == 0 && p1.getBurstTime() == 1
        && p2.getArrivalTime() == 0 && p2.getBurstTime() == 2
        && p3.getArrivalTime() == 1 && p3.getBurstTime() == 1
        && p4.getArrivalTime() == 2 && p4.getBurstTime() == 3
        && p5.getArrivalTime() == 3 && p5.getBurstTime() == 1;
  }

  /** Same process list and quantum as preset “Scenario C - Fairness case” / scenario-C.txt. */
  private static boolean matchesFairnessScenarioC(List<Process> processes, int quantum) {
    if (processes == null || processes.size() != 3 || quantum != 2) {
      return false;
    }
    Map<String, Process> byId = new HashMap<>();
    for (Process p : processes) {
      byId.put(p.getProcessId(), p);
    }
    Process p1 = byId.get("P1");
    Process p2 = byId.get("P2");
    Process p3 = byId.get("P3");
    if (p1 == null || p2 == null || p3 == null) {
      return false;
    }
    return p1.getArrivalTime() == 0 && p1.getBurstTime() == 8
        && p2.getArrivalTime() == 0 && p2.getBurstTime() == 8
        && p3.getArrivalTime() == 0 && p3.getBurstTime() == 8;
  }

  /** Same process list and quantum as preset “Scenario A - Basic mixed workload” / scenario-A.txt. */
  private static boolean matchesBasicMixedWorkloadScenarioA(List<Process> processes, int quantum) {
    if (processes == null || processes.size() != 4 || quantum != 3) {
      return false;
    }
    Map<String, Process> byId = new HashMap<>();
    for (Process p : processes) {
      byId.put(p.getProcessId(), p);
    }
    Process p1 = byId.get("P1");
    Process p2 = byId.get("P2");
    Process p3 = byId.get("P3");
    Process p4 = byId.get("P4");
    if (p1 == null || p2 == null || p3 == null || p4 == null) {
      return false;
    }
    return p1.getArrivalTime() == 0 && p1.getBurstTime() == 6
        && p2.getArrivalTime() == 1 && p2.getBurstTime() == 4
        && p3.getArrivalTime() == 2 && p3.getBurstTime() == 2
        && p4.getArrivalTime() == 3 && p4.getBurstTime() == 5;
  }

  /** Same process list and quantum as preset “Scenario D - Long-job sensitivity case” / scenario-D.txt. */
  private static boolean matchesLongJobSensitivityScenarioD(List<Process> processes, int quantum) {
    if (processes == null || processes.size() != 4 || quantum != 4) {
      return false;
    }
    Map<String, Process> byId = new HashMap<>();
    for (Process p : processes) {
      byId.put(p.getProcessId(), p);
    }
    Process p1 = byId.get("P1");
    Process p2 = byId.get("P2");
    Process p3 = byId.get("P3");
    Process p4 = byId.get("P4");
    if (p1 == null || p2 == null || p3 == null || p4 == null) {
      return false;
    }
    return p1.getArrivalTime() == 0 && p1.getBurstTime() == 20
        && p2.getArrivalTime() == 1 && p2.getBurstTime() == 2
        && p3.getArrivalTime() == 2 && p3.getBurstTime() == 3
        && p4.getArrivalTime() == 3 && p4.getBurstTime() == 1;
  }

  private static int wtSpread(ScheduleResult result) {
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    for (ScheduleResult.ProcessMetrics pm : result.getProcessMetrics().values()) {
      int wt = pm.getWaitingTime();
      min = Math.min(min, wt);
      max = Math.max(max, wt);
    }
    return max - min;
  }

  public static final class ProcessRow {
    private final SimpleStringProperty processId = new SimpleStringProperty();
    private final SimpleStringProperty arrivalTime = new SimpleStringProperty();
    private final SimpleStringProperty burstTime = new SimpleStringProperty();
    /** True if this row was created via "Add Process" (logged to {@code simulation-inputs.log}). */
    private final boolean manualFromForm;

    public ProcessRow(String processId, String arrivalTime, String burstTime) {
      this(processId, arrivalTime, burstTime, false);
    }

    public ProcessRow(String processId, String arrivalTime, String burstTime, boolean manualFromForm) {
      this.processId.set(processId);
      this.arrivalTime.set(arrivalTime);
      this.burstTime.set(burstTime);
      this.manualFromForm = manualFromForm;
    }

    public boolean isManualFromForm() {
      return manualFromForm;
    }

    public String getProcessId() {
      return processId.get();
    }

    public String getArrivalTime() {
      return arrivalTime.get();
    }

    public String getBurstTime() {
      return burstTime.get();
    }

    public SimpleStringProperty processIdProperty() {
      return processId;
    }

    public SimpleStringProperty arrivalTimeProperty() {
      return arrivalTime;
    }

    public SimpleStringProperty burstTimeProperty() {
      return burstTime;
    }
  }

  public static final class ReadyQueueRow {
    private final SimpleStringProperty timeTick = new SimpleStringProperty();
    private final List<String> queueOrder;

    public ReadyQueueRow(String timeTick, List<String> queueOrder) {
      this.timeTick.set(timeTick);
      this.queueOrder = List.copyOf(queueOrder);
    }

    public List<String> getQueueOrder() {
      return queueOrder;
    }

    /** Plain text for sorting / accessibility. */
    public String getQueueSummaryText() {
      return queueOrder.isEmpty() ? "(idle)" : String.join(" → ", queueOrder);
    }

    public SimpleStringProperty timeTickProperty() {
      return timeTick;
    }
  }
}
