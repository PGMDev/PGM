package tc.oc.pgm.score;

import java.util.AbstractMap;
import java.util.Map;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.event.CompetitorScoreChangeEvent;
import tc.oc.pgm.util.Pair;

public class MercyRule {

  private final ScoreMatchModule scoreMatchModule;
  private final int scoreLimit;
  private final int mercyLimit;
  private final int mercyLimitMin;

  private Pair<Competitor, Double> leader;
  private Pair<Competitor, Double> trailer;

  public MercyRule(
      ScoreMatchModule scoreMatchModule, int scoreLimit, int mercyLimit, int mercyLimitMin) {
    this.scoreMatchModule = scoreMatchModule;
    this.scoreLimit = scoreLimit;
    this.mercyLimit = mercyLimit;
    this.mercyLimitMin = mercyLimitMin;

    calculateLeaders();
  }

  private double getLeaderScore() {
    return leader.getRight();
  }

  private double getTrailerScore() {
    return trailer.getRight();
  }

  private void setLeader(Competitor competitor, Double score) {
    leader = Pair.of(competitor, score);
  }

  private void setTrailer(Competitor competitor, Double score) {
    trailer = Pair.of(competitor, score);
  }

  private boolean isLeader(Competitor competitor) {
    return competitor.equals(leader.getLeft());
  }

  private boolean isTrailer(Competitor competitor) {
    return competitor.equals(trailer.getLeft());
  }

  public int getScoreLimit() {
    int scoreBaseline = (int) getTrailerScore();

    int mercyBaseline = Math.max(scoreBaseline + mercyLimit, mercyLimitMin);

    if (scoreLimit < 0) {
      return mercyBaseline;
    }

    return Math.min(mercyBaseline, scoreLimit);
  }

  public void handleEvent(CompetitorScoreChangeEvent event) {
    // Score is larger than leading score
    if (event.getNewScore() > getLeaderScore()) {
      if (!isLeader(event.getCompetitor())) {
        trailer = leader;
      }
      setLeader(event.getCompetitor(), event.getNewScore());
      return;
    }

    // Score larger than trailing score
    if (event.getNewScore() > getTrailerScore()) {
      if (!isLeader(event.getCompetitor())) {
        setTrailer(event.getCompetitor(), event.getNewScore());
      }
      return;
    }

    // Score is going down
    if (event.getOldScore() > event.getNewScore()) {
      if (isLeader(event.getCompetitor())
          || isTrailer(event.getCompetitor())
          || trailer.getLeft() == null) {
        calculateLeaders();
      }
    }
  }

  private void calculateLeaders() {
    Map.Entry<Competitor, Double> lead =
        new AbstractMap.SimpleEntry<>(null, Double.NEGATIVE_INFINITY);
    Map.Entry<Competitor, Double> trail =
        new AbstractMap.SimpleEntry<>(null, Double.NEGATIVE_INFINITY);

    for (Map.Entry<Competitor, Double> entry : scoreMatchModule.getScores().entrySet()) {
      if (entry.getValue() > lead.getValue()) {
        trail = lead;
        lead = entry;
      } else if (entry.getValue() > trail.getValue()) {
        trail = entry;
      }
    }

    setLeader(lead.getKey(), lead.getKey() == null ? 0 : lead.getValue());
    setTrailer(trail.getKey(), trail.getKey() == null ? 0 : trail.getValue());
  }
}
