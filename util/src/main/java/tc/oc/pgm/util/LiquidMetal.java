package tc.oc.pgm.util;

import java.util.Locale;

/**
 * Direct port of the JavaScript LiquidMetal. https://github.com/rmm5t/liquidmetal
 *
 * @author Steve Anton
 */
public final class LiquidMetal {
  private LiquidMetal() {}

  public static final double SCORE_NO_MATCH = 0.0;

  // Perfect score, the char is in the abbreviation
  public static final double SCORE_MATCH = 1.0;
  // The next char is the start of the next word, consider the word a good match
  public static final double SCORE_WORD = 0.9;
  // The abbreviation ended but we started the word
  public static final double SCORE_TRAIL_WORD = 0.85;
  // Word not part of the abbreviation
  public static final double SCORE_TRAILING_BUT_STARTED = 0.80;
  public static final double SCORE_TRAILING = 0.75;

  // Minimum value to consider this a valid match, values under this won't match
  public static final double MIN_MATCH = 0.75;

  /**
   * Check if an abbreviation matches a given string
   *
   * @param string The string to match
   * @param abbreviation The shorter version typed in by the user
   * @return true if abbreviation can be turned into string, false otherwise
   */
  public static boolean match(String string, String abbreviation) {
    return score(string, abbreviation) >= MIN_MATCH;
  }

  /**
   * Get the last matching index in {@param string} given an {@param abbreviation}
   *
   * @param string The string to match
   * @param abbreviation The shorter version typed in by the user
   * @return the last index in {@param string} matches, or -1 if there's no match
   */
  public static int getIndexOf(String string, String abbreviation) {
    if (abbreviation == null || abbreviation.length() == 0) return 0;
    if (abbreviation.length() > string.length()) return -1;
    Score score = new Score();
    int result = matchString(score, string, abbreviation);
    return result != -1 && score.avg() >= MIN_MATCH ? result : -1;
  }

  /**
   * Get a score for how good of a match abbreviation is to string
   *
   * @param string the string to match
   * @param abbreviation The shorter version typed in by the user
   * @return a number between 0.0 and 1.0, 0 being no match and 1 being perfect match.
   */
  public static double score(String string, String abbreviation) {
    if (abbreviation == null || abbreviation.length() == 0) return SCORE_TRAILING;
    if (abbreviation.length() > string.length()) return SCORE_NO_MATCH;
    Score score = new Score();
    int result = matchString(score, string, abbreviation);
    return result == -1 ? 0 : score.avg();
  }

  private static int matchString(Score score, String string, String abbreviation) {
    String lower = string.toLowerCase(Locale.ROOT);
    String chars = abbreviation.toLowerCase(Locale.ROOT);

    int lastIndex = -1;
    for (int i = 0; i < chars.length(); i++) {
      char c = chars.charAt(i);
      int index = lower.indexOf(c, lastIndex + 1);

      // Char not found, short-circuit out
      if (index == -1) return -1;

      // If there was a jump, try to match word-start, and back-fill words
      if (index != lastIndex + 1) {
        int nextWord = fillWord(string, lower, c, lastIndex, index, score);
        if (nextWord != -1) index = nextWord;
        else score.fill(SCORE_NO_MATCH, lastIndex + 1, index);
      }

      score.add(SCORE_MATCH);
      lastIndex = index;
    }

    int wordEnd = string.indexOf(' ', lastIndex + 1);
    if (wordEnd == -1) wordEnd = string.length();

    score.fill(SCORE_TRAIL_WORD, lastIndex + 1, wordEnd);

    boolean started = lower.charAt(0) == chars.charAt(0);
    double trailingScore = started ? SCORE_TRAILING_BUT_STARTED : SCORE_TRAILING;
    score.fill(trailingScore, wordEnd, string.length());

    // If this does not hold true, some character either wasn't scored, or was scored twice
    assert score.length == string.length();

    return lastIndex;
  }

  // Attempts to recursively find the start of the next word starting with char c
  private static int fillWord(
      String string, String lower, char c, int lastIndex, int index, Score score) {
    if (index == -1) {
      return -1;
    } else if (isSpace(c)) {
      score.fill(SCORE_WORD, lastIndex + 1, index); // Preceding word
      return index;
    } else if (isNewWord(string, index)) {
      score.add(SCORE_MATCH); // Space
      score.fill(SCORE_WORD, lastIndex + 1, index - 1); // Preceding word
      return index;
    } else if (isUpperCase(string, index)) {
      score.fill(SCORE_WORD, lastIndex + 1, index); // Preceding word
      return index;
    } else {
      return fillWord(string, lower, c, lastIndex, lower.indexOf(c, index + 1), score);
    }
  }

  private static boolean isSpace(char c) {
    return (c == ' ' || c == '\t' || c == '_');
  }

  private static boolean isNewWord(String string, int index) {
    if (index == 0) return false;
    return isSpace(string.charAt(index - 1));
  }

  private static boolean isUpperCase(String string, int index) {
    char c = string.charAt(index);
    return ('A' <= c && c <= 'Z');
  }

  /** Represents a fake double array of which you can only get an average */
  private static class Score {
    public double sum;
    public int length;

    public void add(double value) {
      this.sum += value;
      this.length++;
    }

    public void fill(double value, int from, int to) {
      int len = to - from;
      this.sum += value * len;
      this.length += len;
    }

    public double avg() {
      return this.sum / this.length;
    }
  }
}
