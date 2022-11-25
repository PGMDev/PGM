package tc.oc.pgm.match;

import static tc.oc.pgm.util.Assert.assertTrue;

import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;

public class ParticipantStateImpl extends MatchPlayerStateImpl implements ParticipantState {

  protected ParticipantStateImpl(MatchPlayer player) {
    super(player);
    assertTrue(player.getParty() instanceof Competitor, "participant party must be a competitor");
  }

  @Override
  public @NotNull Competitor getParty() {
    return (Competitor) super.getParty();
  }
}
