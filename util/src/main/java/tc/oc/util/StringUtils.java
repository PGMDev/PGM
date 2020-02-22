package tc.oc.util;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.md_5.bungee.api.ChatColor;

public class StringUtils {
  public static final int GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH =
      55; // Will never wrap, even with the largest characters

  /**
   * Shorthand for listToEnglishCompound(list, "", "").
   *
   * @see #listToEnglishCompound(Collection, String, String)
   */
  public static String listToEnglishCompound(Collection<String> list) {
    return listToEnglishCompound(list, "", "");
  }

  /**
   * Converts a list of strings to a nice English list as a string.
   *
   * <p>For example: In: ["Anxuiz", "MonsieurApple", "Plastix"] Out: "Anxuiz, MonsieurApple and
   * Plastix"
   *
   * @param list List of strings to concatenate.
   * @param prefix Prefix to add before each element in the resulting string.
   * @param suffix Suffix to add after each element in the resulting string.
   * @return String version of the list of strings.
   */
  public static String listToEnglishCompound(Collection<?> list, String prefix, String suffix) {
    StringBuilder builder = new StringBuilder();
    int i = 0;
    for (Object str : list) {
      if (i != 0) {
        if (i == list.size() - 1) {
          builder.append(" and ");
        } else {
          builder.append(", ");
        }
      }
      builder.append(prefix).append(str).append(suffix);
      i++;
    }
    return builder.toString();
  }

  /**
   * Shorthand for listToCompound(list, and, "", "").
   *
   * @see #listToCompound(Collection, String, String, String)
   */
  public static final String listToCompound(Collection<String> list, String and) {
    return listToCompound(list, and, "", "");
  }

  /**
   * Converts a list of strings to a nice list as a string.
   *
   * <p>For example: In: ["Anxuiz", "MonsieurApple", "Plastix"] Out: "Anxuiz, MonsieurApple {and}
   * Plastix"
   *
   * @param list List of strings to concatenate.
   * @param and String to be used for "and"
   * @param prefix Prefix to add before each element in the resulting string.
   * @param suffix Suffix to add after each element in the resulting string.
   * @return String version of the list of strings.
   */
  public static String listToCompound(
      Collection<?> list, String and, String prefix, String suffix) {
    StringBuilder builder = new StringBuilder();
    int i = 0;
    for (Object str : list) {
      if (i != 0) {
        if (i == list.size() - 1) {
          builder.append(" ").append(and).append(" ");
        } else {
          builder.append(", ");
        }
      }
      builder.append(prefix).append(str).append(suffix);
      i++;
    }
    return builder.toString();
  }

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

  /**
   * Sanitizes the provided message, removing any non-alphanumeric characters and swapping spaces
   * with the specified string.
   *
   * <p>Examples: sanitize("Hello! :) How are you?", '-') --> "Hello--How-are-you" sanitize("I am
   * great, thank you!", '*') --> "I*am*great*thank*you"
   *
   * @param string The message to be sanitized.
   * @param spaceReplace The string to be substituted for spaces.
   * @return The sanitized string.
   */
  public static String sanitize(String string, String spaceReplace) {
    return string.replaceAll("[^\\dA-Za-z ]", "").replaceAll("\\s+", spaceReplace);
  }

  public static String truncate(String text, int length) {
    return text.substring(0, Math.min(text.length(), length));
  }

  public static String substring(String text, int begin, int end) {
    return text.substring(Math.min(text.length(), begin), Math.min(text.length(), end));
  }

  public static String dashedChatMessage(String message, String dash, String dashPrefix) {
    return dashedChatMessage(message, dash, dashPrefix, null);
  }

  public static String dashedChatMessage(
      String message, String dash, String dashPrefix, String messagePrefix) {
    message = " " + message + " ";
    int dashCount =
        (GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH - ChatColor.stripColor(message).length() - 2)
            / (dash.length() * 2);
    String dashes = dashCount >= 0 ? Strings.repeat(dash, dashCount) : "";

    StringBuffer builder = new StringBuffer();
    if (dashCount > 0) {
      builder.append(dashPrefix).append(dashes).append(ChatColor.RESET);
    }

    if (messagePrefix != null) {
      builder.append(messagePrefix);
    }

    builder.append(message);

    if (dashCount > 0) {
      builder.append(ChatColor.RESET).append(dashPrefix).append(dashes);
    }

    return builder.toString();
  }

  /**
   * Trim a string if it is longer than a certain length.
   *
   * @param str
   * @param len
   * @return
   */
  public static String trimLength(String str, int len) {
    if (str.length() > len) {
      return str.substring(0, len);
    }

    return str;
  }

  /**
   * Join an array of strings into a string.
   *
   * @param str
   * @param delimiter
   * @param initialIndex
   * @return
   */
  public static String joinString(String[] str, String delimiter, int initialIndex) {
    if (str.length == 0) {
      return "";
    }
    StringBuilder buffer = new StringBuilder(str[initialIndex]);
    for (int i = initialIndex + 1; i < str.length; ++i) {
      buffer.append(delimiter).append(str[i]);
    }
    return buffer.toString();
  }

  /**
   * Join an array of strings into a string.
   *
   * @param str
   * @param delimiter
   * @param initialIndex
   * @param quote
   * @return
   */
  public static String joinQuotedString(
      String[] str, String delimiter, int initialIndex, String quote) {
    if (str.length == 0) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    buffer.append(quote);
    buffer.append(str[initialIndex]);
    buffer.append(quote);
    for (int i = initialIndex + 1; i < str.length; ++i) {
      buffer.append(delimiter).append(quote).append(str[i]).append(quote);
    }
    return buffer.toString();
  }

  /**
   * Join an array of strings into a string.
   *
   * @param str
   * @param delimiter
   * @return
   */
  public static String joinString(String[] str, String delimiter) {
    return joinString(str, delimiter, 0);
  }

  /**
   * Join an array of strings into a string.
   *
   * @param str
   * @param delimiter
   * @param initialIndex
   * @return
   */
  public static String joinString(Object[] str, String delimiter, int initialIndex) {
    if (str.length == 0) {
      return "";
    }
    StringBuilder buffer = new StringBuilder(str[initialIndex].toString());
    for (int i = initialIndex + 1; i < str.length; ++i) {
      buffer.append(delimiter).append(str[i].toString());
    }
    return buffer.toString();
  }

  /**
   * Join an array of strings into a string.
   *
   * @param str
   * @param delimiter
   * @param initialIndex
   * @return
   */
  public static String joinString(int[] str, String delimiter, int initialIndex) {
    if (str.length == 0) {
      return "";
    }
    StringBuilder buffer = new StringBuilder(Integer.toString(str[initialIndex]));
    for (int i = initialIndex + 1; i < str.length; ++i) {
      buffer.append(delimiter).append(Integer.toString(str[i]));
    }
    return buffer.toString();
  }

  /**
   * Join an list of strings into a string.
   *
   * @param str
   * @param delimiter
   * @param initialIndex
   * @return
   */
  public static String joinString(Collection<?> str, String delimiter, int initialIndex) {
    if (str.size() == 0) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    int i = 0;
    for (Object o : str) {
      if (i >= initialIndex) {
        if (i > 0) {
          buffer.append(delimiter);
        }

        buffer.append(o.toString());
      }
      ++i;
    }
    return buffer.toString();
  }

  /**
   * Find the Levenshtein distance between two Strings.
   *
   * <p>This is the number of changes needed to change one String into another, where each change is
   * a single character modification (deletion, insertion or substitution).
   *
   * <p>The previous implementation of the Levenshtein distance algorithm was from <a
   * href="http://www.merriampark.com/ld.htm">http://www.merriampark.com/ld.htm</a>
   *
   * <p>Chas Emerick has written an implementation in Java, which avoids an OutOfMemoryError which
   * can occur when my Java implementation is used with very large strings.<br>
   * This implementation of the Levenshtein distance algorithm is from <a
   * href="http://www.merriampark.com/ldjava.htm">http://www.merriampark.com/ldjava.htm</a>
   *
   * <pre>
   * StringUtil.getLevenshteinDistance(null, *)             = IllegalArgumentException
   * StringUtil.getLevenshteinDistance(*, null)             = IllegalArgumentException
   * StringUtil.getLevenshteinDistance("","")               = 0
   * StringUtil.getLevenshteinDistance("","a")              = 1
   * StringUtil.getLevenshteinDistance("aaapppp", "")       = 7
   * StringUtil.getLevenshteinDistance("frog", "fog")       = 1
   * StringUtil.getLevenshteinDistance("fly", "ant")        = 3
   * StringUtil.getLevenshteinDistance("elephant", "hippo") = 7
   * StringUtil.getLevenshteinDistance("hippo", "elephant") = 7
   * StringUtil.getLevenshteinDistance("hippo", "zzzzzzzz") = 8
   * StringUtil.getLevenshteinDistance("hello", "hallo")    = 1
   * </pre>
   *
   * @param s the first String, must not be null
   * @param t the second String, must not be null
   * @return result distance
   * @throws IllegalArgumentException if either String input <code>null</code>
   */
  public static int getLevenshteinDistance(String s, String t) {
    if (s == null || t == null) {
      throw new IllegalArgumentException("Strings must not be null");
    }

    /*
     * The difference between this impl. and the previous is that, rather
     * than creating and retaining a matrix of size s.length()+1 by
     * t.length()+1, we maintain two single-dimensional arrays of length
     * s.length()+1. The first, d, is the 'current working' distance array
     * that maintains the newest distance cost counts as we iterate through
     * the characters of String s. Each time we increment the index of
     * String t we are comparing, d is copied to p, the second int[]. Doing
     * so allows us to retain the previous cost counts as required by the
     * algorithm (taking the minimum of the cost count to the left, up one,
     * and diagonally up and to the left of the current cost count being
     * calculated). (Note that the arrays aren't really copied anymore, just
     * switched...this is clearly much better than cloning an array or doing
     * a System.arraycopy() each time through the outer loop.)
     *
     * Effectively, the difference between the two implementations is this
     * one does not cause an out of memory condition when calculating the LD
     * over two very large strings.
     */

    int n = s.length(); // length of s
    int m = t.length(); // length of t

    if (n == 0) {
      return m;
    } else if (m == 0) {
      return n;
    }

    int p[] = new int[n + 1]; // 'previous' cost array, horizontally
    int d[] = new int[n + 1]; // cost array, horizontally
    int _d[]; // placeholder to assist in swapping p and d

    // indexes into strings s and t
    int i; // iterates through s
    int j; // iterates through t

    char tj; // jth character of t

    int cost; // cost

    for (i = 0; i <= n; ++i) {
      p[i] = i;
    }

    for (j = 1; j <= m; ++j) {
      tj = t.charAt(j - 1);
      d[0] = j;

      for (i = 1; i <= n; ++i) {
        cost = s.charAt(i - 1) == tj ? 0 : 1;
        // minimum of cell to the left+1, to the top+1, diagonally left
        // and up +cost
        d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
      }

      // copy current distance counts to 'previous row' distance counts
      _d = p;
      p = d;
      d = _d;
    }

    // our last action in the above loop was to switch d and p, so p now
    // actually has the most recent cost counts
    return p[n];
  }

  public static <T extends Enum<?>> T lookup(Map<String, T> lookup, String name, boolean fuzzy) {
    String testName = name.replaceAll("[ _]", "").toLowerCase();

    T type = lookup.get(testName);
    if (type != null) {
      return type;
    }

    if (!fuzzy) {
      return null;
    }

    int minDist = -1;

    for (Map.Entry<String, T> entry : lookup.entrySet()) {
      final String key = entry.getKey();
      if (key.charAt(0) != testName.charAt(0)) {
        continue;
      }

      int dist = getLevenshteinDistance(key, testName);

      if ((dist < minDist || minDist == -1) && dist < 2) {
        minDist = dist;
        type = entry.getValue();
      }
    }

    return type;
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
    // This is probably the simplest way to get an accurate percentage while never rounding up to
    // 100%
    int percent = (int) Math.round(100.0 * completion);
    if (percent == 100 && completion < 1d) {
      return "99%";
    } else {
      return percent + "%";
    }
  }
}
