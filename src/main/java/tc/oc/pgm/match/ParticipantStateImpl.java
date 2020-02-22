package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkArgument;

import org.bukkit.Location;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.util.bukkit.identity.Identity;

public class ParticipantStateImpl extends MatchPlayerStateImpl implements ParticipantState {

  protected ParticipantStateImpl(Match match, Identity identity, Party party, Location location) {
    super(match, identity, party, location);
    checkArgument(party instanceof Competitor, "participant party must be a competitor");
  }

  @Override
  public Competitor getParty() {
    return (Competitor) super.getParty();
  }
}
