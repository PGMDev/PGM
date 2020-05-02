package tc.oc.pgm.menu;

import java.util.List;
import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.items.ItemHolder;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;

public class InventoryMenuImpl implements InventoryMenu {

  private final WeakHashMap<MatchPlayer, InventoryMenu> history;
  private final List<ItemHolder> items;
  private final int rows;
  private final Component title;

  /**
   * Creates a new {@link InventoryMenuImpl}
   *
   * @param items the items to put in this inventory
   * @param rows the number of rows this inventory will have
   * @param title the title of this inventory
   */
  public InventoryMenuImpl(List<ItemHolder> items, int rows, Component title) {
    this.history = new WeakHashMap<>();
    this.items = items;
    this.rows = rows;
    this.title = title;
  }

  @Override
  public void openAsRoot(MatchPlayer player) {
    history.remove(player);
    openRaw(player);
  }

  @Override
  public void openRaw(MatchPlayer player) {
    Inventory inventory =
        Bukkit.createInventory(
            player, rows * 9, ComponentRenderers.toLegacyText(title, player.getBukkit()));

    populateInventory(player, inventory);

    player.getMatch().getModule(InventoryMenuMatchModule.class).addInventory(inventory, this);
    player.getBukkit().openInventory(inventory);
  }

  private void populateInventory(MatchPlayer player, Inventory inventory) {
    for (ItemHolder item : items) {
      item.putInInventory(player, inventory, this);
    }
  }

  @Override
  public void openWithPrevious(MatchPlayer player, InventoryMenu previous) {
    history.put(player, previous);
    openRaw(player);
  }

  @Override
  public void clickItem(int x, int y, MatchPlayer player, ClickType clickType) {
    for (ItemHolder item : items) {
      if (item.x == x && item.y == y) {
        item.item.onClick(this, player, clickType);
      }
    }
  }

  @Override
  public boolean hasPrevious(MatchPlayer player) {
    return history.containsKey(player);
  }

  @Override
  public void popPrevious(MatchPlayer player) {
    InventoryMenu previous = history.get(player);
    if (previous == null) {
      throw new IllegalStateException("Tried to pop previous inventory but player has no history");
    }

    history.get(player).openRaw(player);
  }

  @Override
  public void purgeAll() {
    for (ItemHolder item : items) {
      item.item.purgeAll();
    }
  }

  @Override
  public void purge(MatchPlayer player) {
    for (ItemHolder item : items) {
      item.item.purge(player);
    }
  }

  @Override
  public void refresh(MatchPlayer player) {
    purge(player);
    Inventory inventory = player.getBukkit().getOpenInventory().getTopInventory();
    inventory.clear();
    populateInventory(player, inventory);
  }
}
