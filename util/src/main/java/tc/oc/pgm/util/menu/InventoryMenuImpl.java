package tc.oc.pgm.util.menu;

import java.util.List;
import net.kyori.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import tc.oc.pgm.util.menu.items.ItemHolder;
import tc.oc.pgm.util.text.TextTranslations;

public class InventoryMenuImpl implements InventoryMenu {

  private final InventoryMenuManager manager;
  private final List<ItemHolder> items;
  private final int rows;
  private final Component title;

  /**
   * Creates a new {@link InventoryMenuImpl}
   *
   * @param manager the inventory manager
   * @param items the items to put in this inventory
   * @param rows the number of rows this inventory will have
   * @param title the title of this inventory
   */
  public InventoryMenuImpl(
      InventoryMenuManager manager, List<ItemHolder> items, int rows, Component title) {
    this.manager = manager;
    this.items = items;
    this.rows = rows;
    this.title = title;
  }

  @Override
  public void open(Player player) {
    Inventory inventory =
        Bukkit.createInventory(player, rows * 9, TextTranslations.translateLegacy(title, player));

    populateInventory(player, inventory);

    manager.addInventory(inventory, this);
    player.openInventory(inventory);
  }

  private void populateInventory(Player player, Inventory inventory) {
    for (ItemHolder item : items) {
      item.putInInventory(player, inventory, this);
    }
  }

  @Override
  public void clickItem(int x, int y, Player player, ClickType clickType) {
    for (ItemHolder item : items) {
      if (item.x == x && item.y == y) {
        item.item.onClick(this, player, clickType);
      }
    }
  }

  @Override
  public void invalidate(Player player) {
    for (ItemHolder item : items) {
      item.item.invalidate(player);
    }

    Inventory inventory = player.getOpenInventory().getTopInventory();
    inventory.clear();
    populateInventory(player, inventory);
  }
}
