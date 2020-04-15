package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkArgument;

import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;

public class ParticipantStateImpl extends MatchPlayerStateImpl implements ParticipantState {

  protected ParticipantStateImpl(MatchPlayer player) {
    super(player);
    checkArgument(
        player.getParty() instanceof Competitor, "participant party must be a competitor");
  }

  @Override
  public Competitor getParty() {
    return (Competitor) super.getParty();
  }
}
