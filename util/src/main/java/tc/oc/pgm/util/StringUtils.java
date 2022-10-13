package tc.oc.pgm.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.bukkit.ChatColor;

public final class StringUtils {
  private StringUtils() {}

  public static <T> T bestFuzzyMatch(String query, Iterable<T> options, double threshold) {
    return bestFuzzyMatch(query, options, Object::toString, threshold);
  }

  public static <T> T bestFuzzyMatch(String query, Map<String, T> choices, double threshold) {
    Map.Entry<String, T> entry =
        bestFuzzyMatch(query, choices.entrySet(), Map.Entry::getKey, threshold);
    return entry == null ? null : entry.getValue();
  }

  public static <T> T bestFuzzyMatch(
      String query, Iterable<T> choices, Function<T, String> toString, double threshold) {
    return bestFuzzyMatch(query, choices.iterator(), toString, threshold);
  }

  public static <T> T bestFuzzyMatch(
      String query, Iterator<T> choices, Function<T, String> toString, double threshold) {
    T bestObj = null;
    double bestScore = 0.0;
    while (choices.hasNext()) {
      T next = choices.next();
      double score = LiquidMetal.score(toString.apply(next), query);
      if (score > bestScore) {
        bestObj = next;
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

  public static String normalize(String text) {
    return text == null
        ? ""
        : Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("[^A-Za-z0-9]", "")
            .toLowerCase(Locale.ROOT);
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

  public static final int MAX_PREFIX = 16; // Max chars in a team prefix
  public static final int MAX_SUFFIX = 16; // Max chars in a team suffix

  /**
   * Split the row text into prefix and suffix, limited to 16 chars each. Because the player name is
   * a color code, we have to restore the color at the split in the suffix. We also have to be
   * careful not to split in the middle of a color code.
   */
  public static String[] splitIntoTeamPrefixAndSuffix(String text) {
    int split = MAX_PREFIX - 1; // Start by assuming there is a color code right on the split
    if (text.length() < MAX_PREFIX || text.charAt(split) != ChatColor.COLOR_CHAR) {
      // If there isn't, we can fit one more char in the prefix
      split++;
    }

    // Split and truncate the text, and restore the color in the suffix
    String prefix = text.substring(0, split);
    String lastColors = ChatColor.getLastColors(prefix);
    String suffix = lastColors + text.substring(split, split + MAX_SUFFIX - lastColors.length());
    return new String[] {prefix, suffix};
  }
}
