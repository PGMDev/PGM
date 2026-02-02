package tc.oc.pgm.api.match;

import java.util.Locale;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

/** Represents the current state of a {@link Match}. */
public enum MatchPhase {

  /**
   * The {@link Match} is loaded, but no countdown has been queued for it to transition to
   * {@link MatchPhase#STARTING}.
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
    return switch (this) {
      case IDLE -> next == STARTING || next == RUNNING;
      case STARTING -> next == RUNNING || next == IDLE || next == STARTING;
      case RUNNING -> next == FINISHED;
      case FINISHED -> false;
    };
  }

  @Override
  public String toString() {
    return this.name().toLowerCase(Locale.ROOT);
  }
}
