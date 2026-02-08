package tc.oc.pgm.api.party.event;

import lombok.Getter;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.score.ScoreCause;

/**
 * Called when the score of a {@link Competitor} changes.
 *
 * @see tc.oc.pgm.score.ScoreMatchModule
 */
@Getter
public class CompetitorScoreChangeEvent extends PartyEvent {

  /** The old score of the {@link Competitor}. */
  private final double oldScore;
  /** The new, and current, score of the {@link Competitor}. */
  private final double newScore;
  /** The {@link ScoreCause} of the change in score. */
  private final ScoreCause cause;

  public CompetitorScoreChangeEvent(
      Competitor competitor, double oldScore, double newScore, ScoreCause cause) {
    super(competitor);
    this.oldScore = oldScore;
    this.newScore = newScore;
    this.cause = cause;
  }

  /**
   * Get the {@link Competitor} for the {@link CompetitorScoreChangeEvent}.
   *
   * @return The {@link Competitor}.
   */
  public final Competitor getCompetitor() {
    return (Competitor) super.getParty();
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
