package tc.oc.pgm.community.events;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.event.ExtendedCancellable;
import tc.oc.pgm.api.player.MatchPlayer;

/** Called immediately AFTER a player runs the report command. */
public class PlayerReportEvent extends ExtendedCancellable {

  private final MatchPlayer sender;
  private final MatchPlayer player;
  private final String reason;

  public PlayerReportEvent(MatchPlayer sender, MatchPlayer player, String reason) {
    this.sender = checkNotNull(sender);
    this.player = checkNotNull(player);
    this.reason = checkNotNull(reason);
  }

  public MatchPlayer getSender() {
    return sender;
  }

  public MatchPlayer getPlayer() {
    return player;
  }

  public String getReason() {
    return reason;
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
