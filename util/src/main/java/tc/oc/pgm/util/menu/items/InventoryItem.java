package tc.oc.pgm.util.menu.items;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.menu.InventoryMenu;

/** Interface that describes a selection in an inventory menu. */
public interface InventoryItem {

  /**
   * Generates an item stack from the inventory item which is in a specific inventory for a specific
   * player. This method is called to generate an {@link ItemStack} to fill {@link
   * org.bukkit.inventory.Inventory} with for the inventory gui api.
   *
   * <p>This method can return null, in which case the item should not be rendered and is treated as
   * the player not having permission/not meeting requirements to see the item
   *
   * @param inventory the inventory that this item is inside
   * @param player the player this item is being generated for
   * @return the generated itemstack, usually used to put in normal bukkit inventories or null if
   *     the item stack should not be rendered
   */
  ItemStack item(InventoryMenu inventory, Player player);

  /**
   * A callback for when this item is clicked inside of an inventory. If the item has not been
   * rendered, ie the item method returned null, this method will not be called if the player clicks
   * on the empty slot where the item should have been
   *
   * @param inventory the inventory this item is inside of
   * @param player the player who clicked the item
   */
  void onClick(InventoryMenu inventory, Player player, ClickType clickType);

  /**
   * This completely clears the item cache, meaning that the next time this item is generated it
   * will not use the cached version but will call {@link #item(InventoryMenu inventory, Player
   * player)}
   */
  void invalidateAll();

  /**
   * This clears the item cache for a specific player, meaning the next time this item is generated
   * for that player it will not used the cached version but will call {@link #item(InventoryMenu
   * inventory, Player player)}
   *
   * @param player the player the cache is being cleared for
   */
  void invalidate(Player player);
}
