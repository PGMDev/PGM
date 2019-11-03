package tc.oc.pgm.modules;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.match.MatchModule;

@ListenerScope(MatchScope.LOADED)
public class InternalMatchModule extends MatchModule implements Listener {

  public InternalMatchModule(Match match) {
    super(match);
  }

  /** Prevent teleporting outside the map */
  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerTeleport(final PlayerTeleportEvent event) {
    if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
      double fromY = event.getFrom().getY();
      double toY = event.getTo().getY();

      if ((fromY >= 0.0D && fromY < 255.0D) && (toY < 0.0D || toY >= 255.0D)) {
        event.setCancelled(true);
      }
    }
  }
}
