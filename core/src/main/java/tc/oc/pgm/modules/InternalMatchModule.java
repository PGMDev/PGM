package tc.oc.pgm.modules;

import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.LOADED)
public class InternalMatchModule implements MatchModule, Listener {

  public InternalMatchModule(Match match) {}

  /** Prevent teleporting outside the map */
  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerTeleport(final PlayerTeleportEvent event) {
    if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
      double fromY = event.getFrom().getY();
      double toY = event.getTo().getY();
      double minY = NMS_HACKS.getMinWorldHeight(event.getTo().getWorld());
      double maxY = NMS_HACKS.getMaxWorldHeight(event.getTo().getWorld()) - 1;

      if ((fromY >= minY && fromY < maxY) && (toY < minY || toY >= maxY)) {
        event.setCancelled(true);
      }
    }
  }
}
