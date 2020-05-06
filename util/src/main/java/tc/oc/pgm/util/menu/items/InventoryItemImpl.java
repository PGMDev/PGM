package tc.oc.pgm.util.menu.items;

import java.util.WeakHashMap;
import java.util.function.BiFunction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.menu.InventoryMenuManager;

public class InventoryItemImpl implements InventoryItem {

  private final InventoryMenuManager manager;
  private final BiFunction<InventoryMenu, Player, ItemStack> itemGenerator;
  private final InventoryClickAction onClick;

  private final WeakHashMap<Player, ItemStack> cache;
  private final int millisDelay;
  private final boolean shouldCache;

  /**
   * Creates a new {@link InventoryItemImpl}, the default implementation of {@link InventoryItem}
   *
   * @param manager the inventory menu manager
   * @param itemGenerator the function used to create the item
   * @param onClick the onclick function
   * @param millisDelay the delay between a player clicking an item in the inventory and the
   *     callback being executed
   * @param shouldCache whether or not the item should be cached, true if it should be
   */
  public InventoryItemImpl(
      InventoryMenuManager manager,
      BiFunction<InventoryMenu, Player, ItemStack> itemGenerator,
      InventoryClickAction onClick,
      int millisDelay,
      boolean shouldCache) {
    this.manager = manager;
    this.itemGenerator = itemGenerator;
    this.onClick = onClick;
    this.cache = new WeakHashMap<>();
    this.millisDelay = millisDelay;
    this.shouldCache = shouldCache;
  }

  @Override
  public ItemStack item(InventoryMenu inventory, Player player) {
    if (cache.containsKey(player)) {
      return cache.get(player);
    }

    ItemStack stack = itemGenerator.apply(inventory, player);
    if (shouldCache) {
      cache.put(player, stack);
    }

    return stack;
  }

  @Override
  public void onClick(InventoryMenu inventory, Player player, ClickType clickType) {
    if (millisDelay > 0) {
      Bukkit.getScheduler()
          .runTaskLater(
              manager.getPlugin(),
              () -> onClick.onClick(inventory, player, clickType),
              millisDelay / 50);
    } else {
      onClick.onClick(inventory, player, clickType);
    }
  }

  @Override
  public void purgeAll() {
    cache.clear();
  }

  @Override
  public void purge(Player player) {
    cache.remove(player);
  }
}
