package tc.oc.pgm.menu.items;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;

public class InventoryItemBuilder {
  // TODO: Add back button
  private final BiFunction<InventoryMenu, MatchPlayer, ItemStack> createItem;
  private boolean cache;
  private InventoryClickAction onClick;
  private int tickDelay;

  /**
   * Creates a new inventory item builder, from a function that creates the item stack from a
   * specific player
   *
   * @param createItem the create item function
   */
  private InventoryItemBuilder(BiFunction<InventoryMenu, MatchPlayer, ItemStack> createItem) {
    this.createItem = createItem;
    this.cache = false;
    this.tickDelay = 0;
    this.onClick = (menu, player, clickType) -> {};
  }

  /**
   * Creates a new {@link InventoryItemBuilder} that is used to create an {@link InventoryItem}
   *
   * @param createItem the create item function that constructs an {@link ItemStack} from the
   *     inventory and the player
   * @return the created {@link InventoryItemBuilder}
   */
  public static InventoryItemBuilder createItem(
      BiFunction<InventoryMenu, MatchPlayer, ItemStack> createItem) {
    return new InventoryItemBuilder(createItem);
  }

  /**
   * Creates a new {@link InventoryItemBuilder}, used to create inventory items
   *
   * @param item an {@link ItemStack} that should be in the inventory
   * @return the created {@link InventoryItemBuilder}
   */
  public static InventoryItemBuilder createItem(ItemStack item) {
    return new InventoryItemBuilder((x, y) -> item);
  }

  /**
   * Sets the on click callback, the function that is called when the player clicks the item in the
   * inventory
   *
   * @param onClick the callback that takes two parameters, the inventory the item is in and the
   *     player who clicked the item
   * @return the builder
   */
  public InventoryItemBuilder onClick(BiConsumer<InventoryMenu, MatchPlayer> onClick) {
    this.onClick = (menu, player, clickType) -> onClick.accept(menu, player);
    return this;
  }

  /**
   * Sets the on click callback, the function that is called when the player clicks the item in the
   * inventory
   *
   * @param clickAction the callback that takes three parameters, the inventory the item is in, the
   *     player who clicked the item, and the type of click which is occurring
   * @return the builder
   */
  public InventoryItemBuilder onClick(InventoryClickAction clickAction) {
    this.onClick = clickAction;
    return this;
  }

  /**
   * Sets the tick delay, affects the delay before the call back is called after an item is clicked
   * in the inventory
   *
   * @param tickDelay the delay in ticks to run the callback after the item in question is clicked
   * @return the builder
   */
  public InventoryItemBuilder setDelay(int tickDelay) {
    this.tickDelay = tickDelay;
    return this;
  }

  /**
   * Whether or not the item should be cached to improve performance
   *
   * @param shouldCache whether or not the item should be cached
   * @return the builder
   */
  public InventoryItemBuilder shouldCache(boolean shouldCache) {
    this.cache = shouldCache;
    return this;
  }

  /**
   * Creates a new {@link InventoryItem} from the builder
   *
   * @return the {@link InventoryItem} that has been created by this builder
   */
  public InventoryItem build() {
    return new InventoryItemImpl(createItem, onClick, tickDelay, cache);
  }
}
