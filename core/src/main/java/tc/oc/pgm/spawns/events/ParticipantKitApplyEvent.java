package tc.oc.pgm.spawns.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

/**
 * Called when it is safe to apply post-spawn kits to a player. Can be used by other plugins to
 * apply kits upon spawning
 */
public class ParticipantKitApplyEvent extends MatchPlayerEvent {

  public ParticipantKitApplyEvent(MatchPlayer player) {
    super(player);
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
