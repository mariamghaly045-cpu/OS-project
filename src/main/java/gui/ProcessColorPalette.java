package gui;

import javafx.scene.paint.Color;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consistent per-process colors for Gantt blocks and result table rows (dashboard-style UI).
 */
public final class ProcessColorPalette {

  private static final Pattern PID_NUM = Pattern.compile("(\\d+)");

  private static final String[] HEX_COLORS = {
      "#E74C3C", // P-style palette: distinct, high contrast
      "#F39C12",
      "#2ECC71",
      "#9B59B6",
      "#3498DB",
      "#1ABC9C",
      "#E67E22",
      "#8E44AD",
      "#2980B9",
      "#C0392B",
      "#16A085",
      "#D35400",
      "#27AE60",
      "#7F8C8D",
      "#2C3E50"
  };

  private ProcessColorPalette() {}

  public static Color fillForBlock(String processId) {
    if (processId == null) {
      return Color.web("#BDC3C7");
    }
    String p = processId.trim();
    if (p.isEmpty() || "Idle".equalsIgnoreCase(p)) {
      return Color.web("#BDC3C7");
    }
    int idx = indexFromPid(p);
    if (idx < 0) {
      idx = Math.floorMod(p.hashCode(), HEX_COLORS.length);
    }
    return Color.web(HEX_COLORS[idx % HEX_COLORS.length]);
  }

  /**
   * Darker stroke around Gantt segments.
   */
  public static Color strokeForBlock(String processId) {
    Color c = fillForBlock(processId);
    return c.darker().darker();
  }

  /**
   * Text color that contrasts with the Gantt fill.
   */
  public static Color textColorForGanttFill(Color fill) {
    double r = fill.getRed();
    double g = fill.getGreen();
    double b = fill.getBlue();
    double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
    return luminance > 0.62 ? Color.web("#1a1a1a") : Color.WHITE;
  }

  /**
   * Light tinted background for TableView rows (CSS).
   */
  public static String tableRowStyle(String processId) {
    Color c = fillForBlock(processId);
    Color light =
        Color.color(
            c.getRed() * 0.12 + 0.88,
            c.getGreen() * 0.12 + 0.88,
            c.getBlue() * 0.12 + 0.88);
    return String.format(
        Locale.US,
        "-fx-background-color: rgb(%d, %d, %d);",
        (int) Math.round(light.getRed() * 255),
        (int) Math.round(light.getGreen() * 255),
        (int) Math.round(light.getBlue() * 255));
  }

  private static int indexFromPid(String pid) {
    Matcher m = PID_NUM.matcher(pid);
    if (m.find()) {
      try {
        return Integer.parseInt(m.group(1)) - 1;
      } catch (NumberFormatException ignored) {
        return -1;
      }
    }
    return -1;
  }

  /** CSS rgb(...) for inline styles (chips, labels). */
  public static String cssRgb(Color c) {
    return String.format(
        Locale.US,
        "rgb(%d,%d,%d)",
        (int) Math.round(c.getRed() * 255),
        (int) Math.round(c.getGreen() * 255),
        (int) Math.round(c.getBlue() * 255));
  }
}
