package tc.oc.pgm.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StringUtils {
  private StringUtils() {}

  public static <T> T bestFuzzyMatch(String search, Iterable<T> options, double threshold) {
    Map<String, T> map = new HashMap<>();
    for (T t : options) {
      map.put(t.toString(), t);
    }

    return bestFuzzyMatch(search, map, threshold);
  }

  public static <T> T bestFuzzyMatch(String query, Map<String, T> choices, double threshold) {
    T bestObj = null;
    double bestScore = 0.0;
    for (Map.Entry<String, T> entry : choices.entrySet()) {
      double score = LiquidMetal.score(entry.getKey(), query);
      if (score > bestScore) {
        bestObj = entry.getValue();
        bestScore = score;
      } else if (score == bestScore) {
        bestObj = null;
      }
    }

    return bestScore < threshold ? null : bestObj;
  }

  public static String truncate(String text, int length) {
    return text.substring(0, Math.min(text.length(), length));
  }

  public static String substring(String text, int begin, int end) {
    return text.substring(Math.min(text.length(), begin), Math.min(text.length(), end));
  }

  public static List<String> complete(String prefix, Iterable<String> options) {
    final String prefixLower = prefix.toLowerCase();
    final int pos = prefixLower.lastIndexOf(' ');
    final List<String> matches = new ArrayList<>();
    options.forEach(
        option -> {
          if (option.toLowerCase().startsWith(prefixLower)) {
            matches.add(pos == -1 ? option : option.substring(pos + 1));
          }
        });
    Collections.sort(matches);
    return matches;
  }

  public static String percentage(double completion) {
    // The simplest way to get an accurate percentage while never rounding up to 100%
    int percent = (int) Math.round(100.0 * completion);
    if (percent == 100 && completion < 1d) {
      return "99%";
    } else {
      return percent + "%";
    }
  }

  public static String camelCase(String text) {
    return camelCase(text, false);
  }

  public static String camelCase(String text, boolean reverse) {
    final String[] sections = text.toLowerCase().split("[-_ ]");
    final StringBuilder builder = new StringBuilder(text.length());
    final int start = reverse ? sections.length - 1 : 0;

    for (int i = 0; i < sections.length; i++) {
      final String section = sections[Math.abs(start - i)];

      builder.append(
          i == 0 ? section : section.substring(0, 1).toUpperCase() + section.substring(1));
    }

    return builder.toString();
  }
}
