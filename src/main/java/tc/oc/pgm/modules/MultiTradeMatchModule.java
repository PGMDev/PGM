package tc.oc.pgm.modules;

import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.world.NMSHacks;

@ListenerScope(MatchScope.LOADED)
public class MultiTradeMatchModule implements MatchModule, Listener {

  public MultiTradeMatchModule(Match match) {}

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void processItemRemoval(PlayerInteractEntityEvent event) {
    if (event.getRightClicked() instanceof Villager) {
      event.setCancelled(true);
      NMSHacks.openVillagerTrade(event.getPlayer(), (Villager) event.getRightClicked());
    }
  }
}
