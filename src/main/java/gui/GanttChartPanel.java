package gui;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import model.ScheduleResult;
import model.ScheduleResult.GanttBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class GanttChartPanel extends VBox {
  private final Label titleLabel = new Label();
  /** Total canvas height: bar + room for up to four timeline label rows. */
  private static final double CANVAS_HEIGHT = 248;
  /** Minimum horizontal pixels per one time unit so ticks stay legible in every scenario. */
  private static final double MIN_PX_PER_TIME_UNIT = 14.0;
  private static final double TIMELINE_ROW_STEP = 15.0;
  private static final int TIMELINE_MAX_ROWS = 4;

  private final Canvas canvas = new Canvas(900, CANVAS_HEIGHT);
  private final ScrollPane chartScroll = new ScrollPane(canvas);
  private ScheduleResult scheduleResult;
  private final String defaultTitle;

  public GanttChartPanel(String defaultTitle) {
    this.defaultTitle = Objects.requireNonNull(defaultTitle, "defaultTitle");
    setSpacing(8);
    setPadding(new Insets(8, 8, 8, 8));
    titleLabel.setText(defaultTitle);
    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
    setStyle(
        "-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #d5d5d5; "
            + "-fx-border-radius: 10; -fx-border-width: 1;");

    BorderPane frame = new BorderPane();
    frame.setPadding(new Insets(0));
    frame.setMinHeight(CANVAS_HEIGHT);
    frame.setPrefHeight(CANVAS_HEIGHT);
    chartScroll.setFitToHeight(true);
    chartScroll.setFitToWidth(false);
    chartScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    chartScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    chartScroll.setPannable(true);
    chartScroll.setStyle("-fx-background: #ffffff; -fx-background-color: #ffffff;");
    frame.setCenter(chartScroll);
    VBox.setVgrow(frame, Priority.NEVER);

    getChildren().addAll(titleLabel, frame);

    ChangeListener<Number> sizeListener = (obs, oldV, newV) -> draw();
    canvas.setHeight(CANVAS_HEIGHT);
    canvas.heightProperty().addListener(sizeListener);
    chartScroll.widthProperty().addListener(sizeListener);
    chartScroll.heightProperty().addListener(sizeListener);
    chartScroll.viewportBoundsProperty().addListener((obs, a, b) -> draw());
  }

  public void setScheduleResult(ScheduleResult scheduleResult) {
    this.scheduleResult = scheduleResult;
    titleLabel.setText(scheduleResult == null ? defaultTitle : scheduleResult.getAlgorithmName() + " Gantt Chart");
    draw();
  }

  private void draw() {
    GraphicsContext g = canvas.getGraphicsContext2D();

    if (scheduleResult == null) {
      double cw0 = Math.max(1, canvas.getWidth());
      double ch0 = Math.max(1, canvas.getHeight());
      g.clearRect(0, 0, cw0, ch0);
      g.setFill(Color.DIMGRAY);
      g.setFont(javafx.scene.text.Font.font(13));
      g.fillText("Run simulation to render Gantt chart.", 10, 24);
      return;
    }

    List<GanttBlock> blocks = scheduleResult.getGanttBlocks();
    int totalTime = scheduleResult.getTotalTime();
    if (blocks.isEmpty() || totalTime <= 0) {
      double cw0 = Math.max(1, canvas.getWidth());
      double ch0 = Math.max(1, canvas.getHeight());
      g.clearRect(0, 0, cw0, ch0);
      g.fillText("No schedule to display.", 10, 20);
      return;
    }

    double leftPadding = 20;
    double rightPadding = 28;
    double topPadding = 28;
    double timelineReserve = 12 + TIMELINE_MAX_ROWS * TIMELINE_ROW_STEP + 6;

    double vb = chartScroll.getViewportBounds().getWidth();
    double viewportW = vb > 1 ? vb : Math.max(280, chartScroll.getWidth() - 18);
    if (viewportW <= 0 || Double.isNaN(viewportW)) {
      viewportW = 400;
    }
    double availableInView = Math.max(1, viewportW - leftPadding - rightPadding);
    double naturalPx = availableInView / totalTime;
    double pxPerTime = Math.max(naturalPx, MIN_PX_PER_TIME_UNIT);
    double contentDrawWidth = leftPadding + rightPadding + totalTime * pxPerTime;
    double canvasW = Math.ceil(contentDrawWidth);
    canvas.setWidth(canvasW);
    canvas.setHeight(CANVAS_HEIGHT);
    double h = canvas.getHeight();

    g.clearRect(0, 0, canvasW, h);

    double barTop = topPadding;
    double barHeight = Math.max(56, h - topPadding - timelineReserve);
    double timelineY = barTop + barHeight + 10;

    g.setFill(Color.WHITE);
    g.fillRect(0, 0, canvasW, h);

    // Tick marks at every boundary; one numeric label per tick (two rows / smaller font if tight).
    List<Integer> boundaries = new ArrayList<>();
    boundaries.add(0);
    for (GanttBlock b : blocks) {
      boundaries.add(b.getStartTime());
      boundaries.add(b.getEndTime());
    }
    boundaries.sort(Comparator.naturalOrder());
    boundaries = boundaries.stream().distinct().toList();

    g.setStroke(Color.web("#bdbdbd"));
    g.setLineWidth(1);
    for (int t : boundaries) {
      double x = leftPadding + t * pxPerTime;
      g.strokeLine(x, barTop, x, timelineY - 10);
    }

    paintTimelineLabels(g, canvasW, boundaries, leftPadding, timelineY, pxPerTime);

    // Colored blocks — labels centered; narrow slices use smaller font / outside label.
    for (GanttBlock b : blocks) {
      int start = b.getStartTime();
      int end = b.getEndTime();
      double x = leftPadding + start * pxPerTime;
      double blockW = Math.max(1, (end - start) * pxPerTime);

      Color fill =
          b.isIdle() ? Color.web("#CFD8DC") : ProcessColorPalette.fillForBlock(b.getProcessId());
      Color stroke =
          b.isIdle() ? Color.web("#78909C") : ProcessColorPalette.strokeForBlock(b.getProcessId());

      g.setFill(fill);
      g.setStroke(stroke);
      g.setLineWidth(1.2);
      g.fillRect(x, barTop, blockW, barHeight);
      g.strokeRect(x, barTop, blockW, barHeight);

      String label = b.getDisplayLabel();
      Color tc =
          b.isIdle() ? Color.web("#37474F") : ProcessColorPalette.textColorForGanttFill(fill);
      paintBlockLabel(g, label, tc, x, barTop, blockW, barHeight);
    }
  }

  /**
   * One numeric label per time boundary. Uses up to {@link #TIMELINE_MAX_ROWS} staggered rows and
   * shifts labels along the row only when needed so nothing overlaps; keeps the last tick visible.
   */
  private static void paintTimelineLabels(
      GraphicsContext g,
      double canvasWidth,
      List<Integer> boundaries,
      double leftPadding,
      double timelineY,
      double pxPerTime) {

    g.setFill(Color.web("#555555"));
    final double margin = 3;
    final double gap = 4;

    int[] fontCandidates = {11, 10, 9, 8, 7, 6};

    for (int fontPx : fontCandidates) {
      g.setFont(Font.font(fontPx));

      int n = boundaries.size();
      int[] row = new int[n];
      double[] lxArr = new double[n];

      double[] lastRight = new double[TIMELINE_MAX_ROWS];
      for (int r = 0; r < TIMELINE_MAX_ROWS; r++) {
        lastRight[r] = -1e9;
      }

      boolean ok = true;
      for (int i = 0; i < n; i++) {
        int t = boundaries.get(i);
        String s = String.valueOf(t);
        double tw = approximateTextWidth(s, g.getFont());
        double cx = leftPadding + t * pxPerTime;
        double ideal = cx - tw / 2;

        int chosenRow = -1;
        double chosenLx = ideal;

        for (int r = 0; r < TIMELINE_MAX_ROWS; r++) {
          double lx = Math.max(ideal, lastRight[r] + gap);
          lx = clamp(lx, margin, Math.max(margin, canvasWidth - tw - margin));
          if (lx + tw <= canvasWidth - margin + 1e-6) {
            chosenRow = r;
            chosenLx = lx;
            break;
          }
        }

        if (chosenRow < 0) {
          ok = false;
          break;
        }

        row[i] = chosenRow;
        lxArr[i] = chosenLx;
        lastRight[chosenRow] = chosenLx + tw;
      }

      if (ok) {
        for (int i = 0; i < n; i++) {
          double y = timelineY + 12 + row[i] * TIMELINE_ROW_STEP;
          g.fillText(String.valueOf(boundaries.get(i)), lxArr[i], y);
        }
        return;
      }
    }

    g.setFont(Font.font(6));
    double[] lastRight = new double[TIMELINE_MAX_ROWS];
    for (int r = 0; r < TIMELINE_MAX_ROWS; r++) {
      lastRight[r] = -1e9;
    }
    for (int t : boundaries) {
      String s = String.valueOf(t);
      double tw = approximateTextWidth(s, g.getFont());
      double cx = leftPadding + t * pxPerTime;
      double ideal = cx - tw / 2;
      int chosen = 0;
      double lx = ideal;
      boolean placed = false;
      for (int r = 0; r < TIMELINE_MAX_ROWS; r++) {
        lx = Math.max(ideal, lastRight[r] + gap);
        lx = clamp(lx, margin, Math.max(margin, canvasWidth - tw - margin));
        if (lx + tw <= canvasWidth - margin + 1e-6) {
          chosen = r;
          placed = true;
          break;
        }
      }
      if (!placed) {
        lx = clamp(ideal, margin, Math.max(margin, canvasWidth - tw - margin));
        chosen = 0;
      }
      g.fillText(s, lx, timelineY + 12 + chosen * TIMELINE_ROW_STEP);
      lastRight[chosen] = lx + tw;
    }
  }

  private static double clamp(double v, double lo, double hi) {
    return Math.max(lo, Math.min(hi, v));
  }

  /** Conservative width heuristic so timeline layout rarely underestimates (avoids visual overlap). */
  private static double approximateTextWidth(String s, Font f) {
    if (s == null || s.isEmpty()) {
      return 0;
    }
    double px = f.getSize() * 0.62;
    return s.length() * px + f.getSize() * 0.35;
  }

  /**
   * Horizontally centered; vertically centered via baseline heuristic. Narrow blocks still get text
   * (shrunk font) or clipped label above slice.
   */
  private static void paintBlockLabel(
      GraphicsContext g, String label, Color textColor, double x, double barTop,
      double blockW, double barHeight) {

    if ("Idle".equals(label)) {
      if (blockW >= 36) {
        drawCenteredChip(g, label, textColor, x, barTop, blockW, barHeight, 12);
      }
      return;
    }

    if (blockW < 18 && blockW >= 8) {
      g.save();
      g.translate(x + blockW / 2.0, barTop + barHeight / 2.0);
      g.rotate(-90);
      Font small = Font.font(Math.max(9, Math.min(11, barHeight / 8.0)));
      g.setFont(small);
      g.setFill(textColor);
      double tw = approximateTextWidth(label, small);
      g.fillText(label, -tw / 2.0, 4);
      g.restore();
      return;
    }
    if (blockW < 8) {
      return;
    }

    int len = Math.max(label.length(), 2);
    double fromWidth = blockW / (len * 0.58);
    double fontSize = Math.min(19, Math.max(11, Math.min(barHeight * 0.44, fromWidth)));
    drawCenteredChip(g, label, textColor, x, barTop, blockW, barHeight, fontSize);
  }

  private static void drawCenteredChip(
      GraphicsContext g,
      String label,
      Color textColor,
      double x,
      double barTop,
      double blockW,
      double barHeight,
      double fontSize) {

    Font font = Font.font(fontSize);
    g.setFont(font);
    g.setFill(textColor);

    double tw = approximateTextWidth(label, font);
    double lx = clamp(x + (blockW - tw) / 2, x + 2, Math.max(x + 2, x + blockW - tw - 2));
    double baselineY = verticalCenterBaseline(barTop, barHeight, fontSize);
    if (lx + tw <= x + blockW - 1) {
      g.fillText(label, lx, baselineY);
      return;
    }
    Font smaller = Font.font(Math.max(fontSize - 4, 9));
    g.setFont(smaller);
    tw = approximateTextWidth(label, smaller);
    lx = clamp(x + (blockW - tw) / 2, x + 1, x + Math.max(blockW - tw - 1, 2));
    g.fillText(label, lx, verticalCenterBaseline(barTop, barHeight, smaller.getSize()));
  }

  /**
   * fillText(Y) is baseline: offset from geometric center empirically for Latin cap height.
   */
  private static double verticalCenterBaseline(double barTop, double barHeight, double fontSize) {
    return barTop + (barHeight + fontSize * 0.72) / 2.0;
  }
}
