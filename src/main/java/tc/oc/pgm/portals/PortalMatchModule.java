package tc.oc.pgm.portals;

import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.CoarsePlayerMoveEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.match.MatchModule;

@ListenerScope(MatchScope.LOADED)
public class PortalMatchModule extends MatchModule implements Listener {
  protected final Set<Portal> portals;

  public PortalMatchModule(Match match, Set<Portal> portals) {
    super(match);
    this.portals = portals;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void checkPortalEntry(CoarsePlayerMoveEvent event) {
    if (event.getCause() instanceof PlayerTeleportEvent) {
      return;
    }

    MatchPlayer player = this.match.getPlayer(event.getPlayer());

    for (Portal portal : this.portals) {
      if (portal.teleportEligiblePlayer(
          player, event.getBlockFrom().toVector(), event.getBlockTo().toVector(), event.getTo())) {
        break;
      }
    }
  }
}
