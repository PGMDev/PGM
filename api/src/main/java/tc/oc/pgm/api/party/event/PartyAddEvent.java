package tc.oc.pgm.api.party.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;

/**
 * Called when a {@link Party} is added to a {@link Match}.
 *
 * @see CompetitorAddEvent
 * @see Match#addParty(Party)
 */
public class PartyAddEvent extends PartyEvent {

  public PartyAddEvent(Party party) {
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
