package tc.oc.pgm.events;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.Party;

/**
 * Called after a {@link MatchPlayer} is constructed, but before it is added to a {@link Party}.
 * This event can be used to change the player's initial party. Beyond that, this event doesn't have
 * much use, because the player, and thus the entire {@link Match}, is in an invalid state when it
 * is called. In most cases, {@link PlayerJoinMatchEvent} is a better choice.
 */
public class MatchPlayerAddEvent extends MatchEvent {

  private final Player player;
  private final UUID playerId;
  private Party initialParty;

  public MatchPlayerAddEvent(Match match, Player player, Party initialParty) {
    super(match);
    this.player = player;
    this.playerId = player.getUniqueId();
    setInitialParty(initialParty);
  }

  public Player getPlayer() {
    return player;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  /**
   * Get the party that this player will join AFTER the event returns. Initially, this will be the
   * result of {@link Match#getDefaultParty()}.
   */
  public Party getInitialParty() {
    return initialParty;
  }

  /** Set the party that this player will join AFTER the event returns. */
  public void setInitialParty(Party initialParty) {
    this.initialParty = checkNotNull(initialParty);
  }

  private static HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
