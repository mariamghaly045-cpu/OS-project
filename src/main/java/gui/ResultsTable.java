package gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

import model.ScheduleResult;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

public final class ResultsTable extends VBox {
  private final Label titleLabel;
  private final TableView<MetricRow> tableView = new TableView<>();

  public ResultsTable(String title) {
    this.titleLabel = new Label(title);
    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
    setPadding(new Insets(10));
    setSpacing(8);
    setStyle(
        "-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #d5d5d5; "
            + "-fx-border-radius: 10; -fx-border-width: 1;");

    TableColumn<MetricRow, String> idCol = new TableColumn<>("Process ID");
    idCol.setCellValueFactory(new PropertyValueFactory<>("processId"));
    idCol.setPrefWidth(120);

    TableColumn<MetricRow, String> wtCol = new TableColumn<>("WT");
    wtCol.setCellValueFactory(new PropertyValueFactory<>("waitingTime"));
    wtCol.setPrefWidth(70);

    TableColumn<MetricRow, String> tatCol = new TableColumn<>("TAT");
    tatCol.setCellValueFactory(new PropertyValueFactory<>("turnaroundTime"));
    tatCol.setPrefWidth(80);

    TableColumn<MetricRow, String> rtCol = new TableColumn<>("RT");
    rtCol.setCellValueFactory(new PropertyValueFactory<>("responseTime"));
    rtCol.setPrefWidth(70);

    tableView.getColumns().addAll(idCol, wtCol, tatCol, rtCol);
    tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    tableView.setRowFactory(
        tv -> {
          TableRow<MetricRow> row = new TableRow<>();
          row.itemProperty()
              .addListener(
                  (obs, oldItem, item) -> {
                    if (item == null) {
                      row.setStyle("");
                      return;
                    }
                    if (item.isAverageRow()) {
                      row.setStyle(
                          "-fx-font-weight: bold; -fx-background-color: #eceff1; -fx-border-color: #cfd8dc;");
                    } else {
                      row.setStyle(ProcessColorPalette.tableRowStyle(item.getProcessId()));
                    }
                  });
          return row;
        });

    getChildren().addAll(titleLabel, tableView);
    VBox.setVgrow(tableView, Priority.ALWAYS);
  }

  public void setScheduleResult(ScheduleResult result) {
    tableView.getItems().clear();
    if (result == null) return;

    Map<String, ScheduleResult.ProcessMetrics> metrics = result.getProcessMetrics();
    metrics.entrySet().stream()
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .forEach(e -> {
          ScheduleResult.ProcessMetrics pm = e.getValue();
          tableView.getItems().add(
              new MetricRow(pm.getProcessId(),
                  String.valueOf(pm.getWaitingTime()),
                  String.valueOf(pm.getTurnaroundTime()),
                  String.valueOf(pm.getResponseTime()),
                  false
              )
          );
        });

    tableView.getItems().add(
        new MetricRow(
            "Average",
            format(result.getAverageWaitingTime()),
            format(result.getAverageTurnaroundTime()),
            format(result.getAverageResponseTime()),
            true
        )
    );
  }

  private static String format(double v) {
    return String.format(Locale.US, "%.2f", v);
  }

  public static final class MetricRow {
    private final String processId;
    private final String waitingTime;
    private final String turnaroundTime;
    private final String responseTime;
    private final boolean averageRow;

    public MetricRow(String processId, String waitingTime, String turnaroundTime, String responseTime, boolean averageRow) {
      this.processId = processId;
      this.waitingTime = waitingTime;
      this.turnaroundTime = turnaroundTime;
      this.responseTime = responseTime;
      this.averageRow = averageRow;
    }

    public String getProcessId() {
      return processId;
    }

    public String getWaitingTime() {
      return waitingTime;
    }

    public String getTurnaroundTime() {
      return turnaroundTime;
    }

    public String getResponseTime() {
      return responseTime;
    }

    public boolean isAverageRow() {
      return averageRow;
    }
  }
}
