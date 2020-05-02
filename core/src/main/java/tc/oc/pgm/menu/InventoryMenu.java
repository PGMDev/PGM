package tc.oc.pgm.menu;

import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.items.InventoryItem;

/** Interface that describes a programmable inventory menu. */
public interface InventoryMenu {

  /**
   * Opens the {@link InventoryMenu} with no history, override a past history if one is already
   * present for a specific {@link MatchPlayer}
   *
   * @param player the player who's opening the inventory
   */
  void openAsRoot(MatchPlayer player);

  /**
   * Opens the {@link InventoryMenu} without override the previous history of this inventory. This
   * is important for when a {@link MatchPlayer} presses the back button in a {@link InventoryMenu},
   * this prevents a back button chain
   *
   * @param player the player
   */
  void openRaw(MatchPlayer player);

  /**
   * Opens the {@link InventoryMenu} and sets another {@link InventoryMenu} to be the history for
   * this {@link MatchPlayer}
   *
   * @param player the player opening the inventory
   * @param previous the previous inventory, the one to put in this {@link InventoryMenu}'s history
   */
  void openWithPrevious(MatchPlayer player, InventoryMenu previous);

  /**
   * Acts as a {@link MatchPlayer} clicking a {@link InventoryItem} in a specific {@link
   * InventoryMenu}
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @param player the player clicking the item
   * @param clickType the type of click which is occurring
   */
  void clickItem(int x, int y, MatchPlayer player, ClickType clickType);

  /**
   * Whether or not the {@link MatchPlayer} has a previous inventory, a history in this inventory
   *
   * @param player the player
   * @return true if the player has a history, false otherwise
   */
  boolean hasPrevious(MatchPlayer player);

  /**
   * Pops a {@link MatchPlayer}'s history, opening the inventory in their history
   *
   * @param player the player's who's history is being popped
   */
  void popPrevious(MatchPlayer player);

  /** Purges all {@link InventoryItem} caches, forcing them to be re-rendered. */
  void purgeAll();

  /**
   * Purges all {@link InventoryItem} caches for a specific {@link MatchPlayer}, forcing them to be
   * re-rendered.
   *
   * @param player the player whose item cache to purge
   */
  void purge(MatchPlayer player);

  /**
   * Re-renders the inventory for a player who is currently viewing it.
   *
   * @param player the player whose inventory should be re-rendered.
   */
  void refresh(MatchPlayer player);
}
