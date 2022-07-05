package tc.oc.pgm.score;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;

public class ScoreOnFilterFactory {

  protected final Filter trigger;
  protected final double score;
  protected final ScoreOnFilterType type;

  public ScoreOnFilterFactory(Filter trigger, double score, ScoreOnFilterType type) {
    this.trigger = trigger;
    this.score = score;
    this.type = type;
  }

  public ScoreOnFilter createScoreOnFilter(Match match) {
    return new ScoreOnFilter(trigger, score, type);
  }
}
