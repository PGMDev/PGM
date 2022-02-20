package tc.oc.pgm.kits;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Fired when any kind of Kit is applied to a player. This is fired once for each terminal node of a
 * Kit tree, and NOT for any intermediate nodes.
 */
public class ApplyKitEvent extends MatchEvent implements Cancellable {
  private boolean cancelled;
  private final MatchPlayer player;
  private final Kit kit;
  private boolean force;

  public ApplyKitEvent(MatchPlayer player, Kit kit, boolean force) {
    super(player.getMatch());
    this.player = player;
    this.kit = kit;
    this.force = force;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  public MatchPlayer getPlayer() {
    return player;
  }

  public Kit getKit() {
    return kit;
  }

  public boolean isForce() {
    return force;
  }

  public void setForce(boolean force) {
    this.force = force;
  }

  /* Handler junk */
  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
