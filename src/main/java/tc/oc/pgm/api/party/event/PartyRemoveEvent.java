package tc.oc.pgm.api.party.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;

/**
 * Called when a {@link Party} is removed from a {@link Match}.
 *
 * @see CompetitorRemoveEvent
 * @see Match#removeParty(Party)
 */
public class PartyRemoveEvent extends PartyEvent {

  public PartyRemoveEvent(Party party) {
    super(party);
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
