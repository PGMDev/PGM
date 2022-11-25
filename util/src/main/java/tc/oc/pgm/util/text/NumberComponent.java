package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.text;

import java.text.DecimalFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class NumberComponent {

  /** Common formats used by stats with decimals */
  public static final DecimalFormat FORMATTER = new DecimalFormat("0.##");

  public static Component number(Number stat) {
    double value = stat.doubleValue();
    boolean useShort = Math.abs(value) >= 10_000;
    return text(
        Double.isNaN(value)
            ? "-"
            : FORMATTER.format(useShort ? value / 1000 : value) + (useShort ? "k" : ""));
  }

  /**
   * Wraps a {@link Number} in a {@link Component} that is colored with the given {@link TextColor}.
   * Rounds the number to a maximum of 2 decimals
   *
   * <p>If the number is NaN "-" is wrapped instead
   *
   * <p>If the number is >= 10000 it will be represented in the thousands (10k, 25.5k, 120.3k etc.)
   *
   * @param stat The number you want wrapped
   * @param color The color you want the number to be
   * @return a colored component wrapping the given number or "-" if NaN
   */
  public static Component number(Number stat, TextColor color) {
    return number(stat).color(color);
  }
}
