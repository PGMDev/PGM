package tc.oc.pgm.api.party.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.party.Competitor;

/**
 * Called when the score of a {@link Competitor} changes.
 *
 * @see tc.oc.pgm.score.ScoreMatchModule
 */
public class CompetitorScoreChangeEvent extends PartyEvent {

  private final double oldScore;
  private final double newScore;

  public CompetitorScoreChangeEvent(Competitor competitor, double oldScore, double newScore) {
    super(competitor);
    this.oldScore = oldScore;
    this.newScore = newScore;
  }

  /**
   * Get the {@link Competitor} for the {@link CompetitorScoreChangeEvent}.
   *
   * @return The {@link Competitor}.
   */
  public final Competitor getCompetitor() {
    return (Competitor) super.getParty();
  }

  /**
   * Get the old score of the {@link Competitor}.
   *
   * @return The old score.
   */
  public double getOldScore() {
    return oldScore;
  }

  /**
   * Get the new, and current, score of the {@link Competitor}.
   *
   * @return The new score.
   */
  public double getNewScore() {
    return this.newScore;
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
