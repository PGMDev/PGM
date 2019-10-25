package tc.oc.pgm.match;

import java.util.UUID;
import org.bukkit.Location;
import tc.oc.identity.Identity;

/**
 * A {@link MatchPlayerState} that can only represent a {@link Competitor}, and records the player's
 * location as part of the state.
 */
public class ParticipantState extends MatchPlayerState {

  public ParticipantState(
      Match match, Identity player, UUID uuid, Competitor competitor, Location location) {
    super(match, player, uuid, competitor, location);
  }

  @Override
  public Competitor getParty() {
    return (Competitor) super.getParty();
  }
}
