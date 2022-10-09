package tc.oc.pgm.community.events;

import static java.util.Objects.requireNonNull;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.event.PreemptiveEvent;

/** Called immediately AFTER a player runs the report command. */
public class PlayerReportEvent extends PreemptiveEvent {

  private final MatchPlayer sender;
  private final MatchPlayer player;
  private final String reason;

  public PlayerReportEvent(MatchPlayer sender, MatchPlayer player, String reason) {
    this.sender = requireNonNull(sender);
    this.player = requireNonNull(player);
    this.reason = requireNonNull(reason);
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
