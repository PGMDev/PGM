package tc.oc.pgm.menu.items;

import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;

public class InventoryItemImpl implements InventoryItem {

  private final BiFunction<InventoryMenu, MatchPlayer, ItemStack> itemGenerator;
  private final InventoryClickAction onClick;

  private final WeakHashMap<MatchPlayer, ItemStack> cache;
  private final int millisDelay;
  private final boolean shouldCache;

  /**
   * Creates a new {@link InventoryItemImpl}, the default implementation of {@link InventoryItem}
   *
   * @param itemGenerator the function used to create the item
   * @param onClick the onclick function
   * @param millisDelay the delay between a player clicking an item in the inventory and the
   *     callback being executed
   * @param shouldCache whether or not the item should be cached, true if it should be
   */
  public InventoryItemImpl(
      BiFunction<InventoryMenu, MatchPlayer, ItemStack> itemGenerator,
      InventoryClickAction onClick,
      int millisDelay,
      boolean shouldCache) {
    this.itemGenerator = itemGenerator;
    this.onClick = onClick;
    this.cache = new WeakHashMap<>();
    this.millisDelay = millisDelay;
    this.shouldCache = shouldCache;
  }

  @Override
  public ItemStack item(InventoryMenu inventory, MatchPlayer player) {
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
  public void onClick(InventoryMenu inventory, MatchPlayer player, ClickType clickType) {
    if (millisDelay > 0) {
      PGM.get()
          .getExecutor()
          .schedule(
              () -> onClick.onClick(inventory, player, clickType),
              millisDelay,
              TimeUnit.MILLISECONDS);
    } else {
      onClick.onClick(inventory, player, clickType);
    }
  }

  @Override
  public void purgeAll() {
    cache.clear();
  }

  @Override
  public void purge(MatchPlayer player) {
    cache.remove(player);
  }
}
