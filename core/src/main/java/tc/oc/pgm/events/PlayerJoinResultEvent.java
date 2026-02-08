package tc.oc.pgm.events;

import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.join.JoinResult;

public class PlayerJoinResultEvent extends MatchPlayerEvent implements Cancellable {

  private boolean cancelled;

  @Getter
  private final JoinResult joinResult;

  @Getter
  private final JoinRequest joinRequest;

  public PlayerJoinResultEvent(MatchPlayer player, JoinResult joinResult, JoinRequest request) {
    super(player);
    this.cancelled = false;
    this.joinResult = joinResult;
    this.joinRequest = request;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
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
