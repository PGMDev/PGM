package tc.oc.pgm.events;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.pgm.match.MatchPlayer;

public abstract class MatchPlayerEvent extends MatchEvent {

  protected final MatchPlayer player;

  public MatchPlayerEvent(MatchPlayer player) {
    super(player.getMatch());
    this.player = checkNotNull(player);
  }

  /** Gets the player who joined the match. */
  public MatchPlayer getPlayer() {
    return this.player;
  }
}
