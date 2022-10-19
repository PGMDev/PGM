package tc.oc.pgm.api.player.event;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;

/**
 * Called after {@link MatchPlayer} is constructed, but before its added to a {@link Party}.
 *
 * <p>At event time, the {@link Match} is in an invalid state. Most cases should use {@link
 * PlayerJoinMatchEvent}.
 */
public class MatchPlayerAddEvent extends MatchPlayerEvent {

  private Party initialParty;

  public MatchPlayerAddEvent(MatchPlayer player, Party initialParty) {
    super(player);
    setInitialParty(initialParty);
  }

  /**
   * Get the {@link Party} that the {@link MatchPlayer} will join after the event occurs.
   *
   * @return The {@link Match#getDefaultParty()}.
   */
  public final Party getInitialParty() {
    return initialParty;
  }

  /**
   * Override the initial {@link Party} the {@link MatchPlayer} will join after the event.
   *
   * @param initialParty The {@link Party} the {@link MatchPlayer} should join.
   */
  public final void setInitialParty(Party initialParty) {
    this.initialParty = assertNotNull(initialParty);
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
