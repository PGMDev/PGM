package tc.oc.pgm.modules;

import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.BlockFilter;

@ListenerScope(MatchScope.RUNNING)
public class ItemDestroyMatchModule implements MatchModule, Listener {
  protected final Set<BlockFilter> itemsToRemove;

  public ItemDestroyMatchModule(Match match, Set<BlockFilter> itemsToRemove) {
    this.itemsToRemove = itemsToRemove;
  }

  @SuppressWarnings("deprecation")
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void processItemRemoval(ItemSpawnEvent event) {
    ItemStack item = event.getEntity().getItemStack();
    for (BlockFilter filter : this.itemsToRemove) {
      if (filter.matches(item.getType(), item.getData().getData())) {
        event.setCancelled(true);
      }
    }
  }
}
