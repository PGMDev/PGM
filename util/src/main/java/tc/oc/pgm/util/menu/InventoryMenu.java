package tc.oc.pgm.util.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.util.menu.items.InventoryItem;

/** Interface that describes a programmable inventory menu. */
public interface InventoryMenu {

  /**
   * Opens the {@link InventoryMenu}.
   *
   * @param player the player opening the inventory
   */
  void open(Player player);

  /**
   * Acts as a {@link Player} clicking a {@link InventoryItem} in a specific {@link InventoryMenu}
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @param player the player clicking the item
   * @param clickType the type of click which is occurring
   */
  void clickItem(int x, int y, Player player, ClickType clickType);

  /**
   * Refreshes the menu for a player, on next tick.
   *
   * @param viewer A player.
   */
  void invalidate(Player viewer);
}
