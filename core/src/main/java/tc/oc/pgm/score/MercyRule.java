package tc.oc.pgm.score;

import java.util.AbstractMap;
import java.util.Map;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.event.CompetitorScoreChangeEvent;

public class MercyRule {

  private final ScoreMatchModule scoreMatchModule;
  private final int scoreLimit;
  private final int mercyLimit;

  private Map.Entry<Competitor, Double> leader;
  private Map.Entry<Competitor, Double> trailer;

  public MercyRule(ScoreMatchModule scoreMatchModule, int scoreLimit, int mercyLimit) {
    this.scoreMatchModule = scoreMatchModule;
    this.scoreLimit = scoreLimit;
    this.mercyLimit = mercyLimit;

    calculateLeaders();
  }

  private double getLeaderScore() {
    return leader.getValue();
  }

  private double getTrailerScore() {
    return trailer.getValue();
  }

  private void setLeader(Competitor competitor, Double score) {
    leader = new AbstractMap.SimpleEntry<>(competitor, score);
  }

  private void setTrailer(Competitor competitor, Double score) {
    trailer = new AbstractMap.SimpleEntry<>(competitor, score);
  }

  private boolean isLeader(Competitor competitor) {
    return competitor.equals(leader.getKey());
  }

  private boolean isTrailer(Competitor competitor) {
    return competitor.equals(trailer.getKey());
  }

  public int getScoreLimit() {
    int scoreBaseline = (int) getTrailerScore();

    if (scoreLimit < 0) {
      return scoreBaseline + mercyLimit;
    }

    return Math.min(scoreBaseline + mercyLimit, scoreLimit);
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
          || trailer.getKey() == null) {
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
