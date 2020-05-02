package tc.oc.pgm.menu.items;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;

/**
 * A holder for {@link InventoryItem} inside of a {@link InventoryMenu}. Keeps track of what slot
 * the item is in and the translation between bukkit raw slots and a x,y coordinate system
 */
public class ItemHolder {

  public final int y;
  public final int x;
  public final InventoryItem item;

  /**
   * Constructs a new item holder
   *
   * @param y the y coordinate of the item
   * @param x the x coordinate of the item
   * @param inventoryItem the inventory item itself
   */
  public ItemHolder(int y, int x, InventoryItem inventoryItem) {
    this.y = y;
    this.x = x;
    this.item = inventoryItem;
  }

  /**
   * Translates the x and y coordinates into a bukkit raw slot
   *
   * @return the bukkit slot number
   */
  public int bukkitSlot() {
    return y * 9 + x;
  }

  /**
   * Puts the item into the requested {@link Inventory} that has been generated for a specific
   * player. If the item returned is null it will not be put into the bukkit inventory. This is not
   * an error but an intended feature to allow for items to not generate under certain cicrumstances
   * (For example, the back button not generating if there is no page to go back to)
   *
   * @param player the player
   * @param bukkitInventory the bukkit inventory to put the item in
   * @param inventory the walrus inventory the item is from
   */
  public void putInInventory(
      MatchPlayer player, Inventory bukkitInventory, InventoryMenu inventory) {
    ItemStack itemStack = item.item(inventory, player);
    if (itemStack == null) {
      return;
    }
    bukkitInventory.setItem(bukkitSlot(), itemStack);
  }
}
