package tc.oc.pgm.match;

import org.bukkit.ChatColor;
import tc.oc.pgm.api.match.Match;

/** A party that observers a match. */
public class ObserverParty extends PartyImpl {

  public ObserverParty(final Match match) {
    super(match, "Observers", ChatColor.AQUA, null);
  }

  @Override
  public boolean isParticipating() {
    return false;
  }

  @Override
  public boolean isObserving() {
    return true;
  }
}
