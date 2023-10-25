package tc.oc.pgm.modules;

import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.query.MaterialQuery;

@ListenerScope(MatchScope.RUNNING)
public class ItemDestroyMatchModule implements MatchModule, Listener {
  protected final Set<Filter> itemsToRemove;

  public ItemDestroyMatchModule(Match match, Set<Filter> itemsToRemove) {
    this.itemsToRemove = itemsToRemove;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void processItemRemoval(ItemSpawnEvent event) {
    ItemStack item = event.getEntity().getItemStack();
    for (Filter filter : this.itemsToRemove) {
      if (filter.response(MaterialQuery.get(item.getData()))) {
        event.setCancelled(true);
      }
    }
  }
}
