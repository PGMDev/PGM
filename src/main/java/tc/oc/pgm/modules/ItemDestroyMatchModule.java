package tc.oc.pgm.modules;

import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.BlockFilter;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.match.MatchScope;

@ListenerScope(MatchScope.RUNNING)
public class ItemDestroyMatchModule extends MatchModule implements Listener {
  protected final Set<BlockFilter> itemsToRemove;

  public ItemDestroyMatchModule(Match match, Set<BlockFilter> itemsToRemove) {
    super(match);
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
