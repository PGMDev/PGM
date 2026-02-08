package tc.oc.pgm.api.player.event;

import static tc.oc.pgm.util.Assert.assertNotNull;

import lombok.Getter;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.api.player.MatchPlayer;

/** Represents a {@link MatchEvent} with a {@link MatchPlayer} involved. */
@Getter
public abstract class MatchPlayerEvent extends MatchEvent {

  private final MatchPlayer player;

  protected MatchPlayerEvent(MatchPlayer player) {
    super(player.getMatch());
    this.player = assertNotNull(player);
  }
}
