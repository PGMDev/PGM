package tc.oc.pgm.util;

/**
 * Direct port of the JavaScript LiquidMetal. https://github.com/rmm5t/liquidmetal
 *
 * @author Steve Anton
 */
public final class LiquidMetal {
  private LiquidMetal() {}

  public static final double SCORE_NO_MATCH = 0.0;
  public static final double SCORE_MATCH = 1.0;
  public static final double SCORE_TRAILING = 0.8;
  public static final double SCORE_TRAILING_BUT_STARTED = 0.9;
  public static final double SCORE_BUFFER = 0.85;

  public static double score(String string, String abbreviation) {
    if (abbreviation == null || abbreviation.length() == 0) return SCORE_TRAILING;
    if (abbreviation.length() > string.length()) return SCORE_NO_MATCH;

    double[] scores = buildScoreArray(string, abbreviation);

    // complete miss:
    if (scores == null) {
      return 0;
    }

    double sum = 0.0;
    for (double score : scores) {
      sum += score;
    }

    return (sum / scores.length);
  }

  private static double[] buildScoreArray(String string, String abbreviation) {
    double[] scores = new double[string.length()];
    String lower = string.toLowerCase();
    String chars = abbreviation.toLowerCase();

    int lastIndex = -1;
    boolean started = false;
    for (int i = 0; i < chars.length(); i++) {
      char c = chars.charAt(i);
      int index = lower.indexOf(c, lastIndex + 1);

      if (index == -1) return null; // signal no match
      if (index == 0) started = true;

      if (isNewWord(string, index)) {
        scores[index - 1] = 1.0;
        fillArray(scores, SCORE_BUFFER, lastIndex + 1, index - 1);
      } else if (isUpperCase(string, index)) {
        fillArray(scores, SCORE_BUFFER, lastIndex + 1, index);
      } else {
        fillArray(scores, SCORE_NO_MATCH, lastIndex + 1, index);
      }

      scores[index] = SCORE_MATCH;
      lastIndex = index;
    }

    double trailingScore = started ? SCORE_TRAILING_BUT_STARTED : SCORE_TRAILING;
    fillArray(scores, trailingScore, lastIndex + 1, scores.length);
    return scores;
  }

  private static boolean isNewWord(String string, int index) {
    if (index == 0) return false;
    char c = string.charAt(index - 1);
    return (c == ' ' || c == '\t');
  }

  private static void fillArray(double[] array, double value, int from, int to) {
    for (int i = from; i < to; i++) {
      array[i] = value;
    }
  }

  private static boolean isUpperCase(String string, int index) {
    char c = string.charAt(index);
    return ('A' <= c && c <= 'Z');
  }
}
