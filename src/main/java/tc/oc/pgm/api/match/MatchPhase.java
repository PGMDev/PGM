package tc.oc.pgm.api.match;

import java.util.Locale;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

/** Represents the current state of a {@link Match}. */
public enum MatchPhase {

  /**
   * The {@link Match} is loaded, but no countdown has been queued for it to transition to {@link
   * MatchPhase#STARTING}.
   */
  IDLE,

  /**
   * The {@link Match} has been queued to transition to {@link MatchPhase#RUNNING} and a countdown
   * is actively progressing.
   */
  STARTING,

  /**
   * The {@link Match} is actively running and {@link MatchPlayer}s can transition from observers to
   * participants.
   */
  RUNNING,

  /**
   * The {@link Match} has stopped running and is now done with an optional {@link Competitor} as
   * the winner.
   */
  FINISHED;

  /**
   * Get whether the current {@link MatchPhase} can transition to the next {@link MatchPhase}.
   *
   * @param next the next {@link MatchPhase}.
   * @return Whether the transition is allowed.
   */
  public boolean canTransitionTo(MatchPhase next) {
    switch (this) {
      case IDLE:
        return next == STARTING || next == RUNNING;
      case STARTING:
        return next == RUNNING || next == IDLE || next == STARTING;
      case RUNNING:
        return next == FINISHED;
      case FINISHED:
        return false;
      default:
        throw new IllegalStateException("Unknown transition state for " + next);
    }
  }

  @Override
  public String toString() {
    return this.name().toLowerCase(Locale.ROOT);
  }
}
