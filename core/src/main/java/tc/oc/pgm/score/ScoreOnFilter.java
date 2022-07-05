package tc.oc.pgm.score;

import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.dynamic.FilterMatchModule;

public class ScoreOnFilter implements FeatureDefinition {

  protected final Filter trigger;
  protected final double score;
  protected final ScoreOnFilterType type;

  public ScoreOnFilter(Filter trigger, double score, ScoreOnFilterType type) {
    this.trigger = trigger;
    this.score = score;
    this.type = type;
  }

  public void load(ScoreMatchModule smm, FilterMatchModule fmm) {
    switch (type) {
      case TEAM:
        fmm.onRise(
            Competitor.class,
            trigger,
            competitor -> {
              smm.incrementScore(competitor, score);
            });
        break;
      case PLAYER:
        fmm.onRise(
            MatchPlayer.class,
            trigger,
            player -> {
              smm.incrementScore(player.getId(), player.getCompetitor(), score);
            });
        break;
    }
  }
}
