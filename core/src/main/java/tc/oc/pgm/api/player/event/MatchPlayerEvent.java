package tc.oc.pgm.api.player.event;

import static java.util.Objects.requireNonNull;

import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.api.player.MatchPlayer;

/** Represents a {@link MatchEvent} with a {@link MatchPlayer} involved. */
public abstract class MatchPlayerEvent extends MatchEvent {

  private final MatchPlayer player;

  protected MatchPlayerEvent(MatchPlayer player) {
    super(player.getMatch());
    this.player = requireNonNull(player);
  }

  /**
   * Get the {@link MatchPlayer} involved in the {@link MatchPlayerEvent}.
   *
   * @return The {@link MatchPlayer}.
   */
  public MatchPlayer getPlayer() {
    return player;
  }
}
