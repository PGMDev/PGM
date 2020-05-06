package tc.oc.pgm.util.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.util.menu.items.InventoryItem;

/** Interface that describes a programmable inventory menu. */
public interface InventoryMenu {

  /**
   * Opens the {@link InventoryMenu} with no history, override a past history if one is already
   * present for a specific {@link Player}
   *
   * @param player the player who's opening the inventory
   */
  void openAsRoot(Player player);

  /**
   * Opens the {@link InventoryMenu} without override the previous history of this inventory. This
   * is important for when a {@link Player} presses the back button in a {@link InventoryMenu}, this
   * prevents a back button chain
   *
   * @param player the player
   */
  void openRaw(Player player);

  /**
   * Opens the {@link InventoryMenu} and sets another {@link InventoryMenu} to be the history for
   * this {@link Player}
   *
   * @param player the player opening the inventory
   * @param previous the previous inventory, the one to put in this {@link InventoryMenu}'s history
   */
  void openWithPrevious(Player player, InventoryMenu previous);

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
   * Whether or not the {@link Player} has a previous inventory, a history in this inventory
   *
   * @param player the player
   * @return true if the player has a history, false otherwise
   */
  boolean hasPrevious(Player player);

  /**
   * Pops a {@link Player}'s history, opening the inventory in their history
   *
   * @param player the player's who's history is being popped
   */
  void popPrevious(Player player);

  /** Purges all {@link InventoryItem} caches, forcing them to be re-rendered. */
  void purgeAll();

  /**
   * Purges all {@link InventoryItem} caches for a specific {@link Player}, forcing them to be
   * re-rendered.
   *
   * @param player the player whose item cache to purge
   */
  void purge(Player player);

  /**
   * Re-renders the inventory for a player who is currently viewing it.
   *
   * @param player the player whose inventory should be re-rendered.
   */
  void refresh(Player player);
}
