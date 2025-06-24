package tc.oc.pgm.modules;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.material.MaterialMatcher;

@ListenerScope(MatchScope.RUNNING)
public class ItemDestroyMatchModule implements MatchModule, Listener {
  protected final MaterialMatcher itemsToRemove;

  public ItemDestroyMatchModule(Match match, MaterialMatcher itemsToRemove) {
    this.itemsToRemove = itemsToRemove;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void processItemRemoval(ItemSpawnEvent event) {
    ItemStack item = event.getEntity().getItemStack();
    if (itemsToRemove.matches(item)) {
      event.setCancelled(true);
    }
  }
}
