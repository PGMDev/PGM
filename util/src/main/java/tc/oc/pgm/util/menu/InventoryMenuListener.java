package tc.oc.pgm.util.menu;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.inventory.InventoryUtils;

public class InventoryMenuListener implements Listener {

  private final Map<Inventory, InventoryMenu> inventoryMap;
  private final Plugin plugin;

  /** Creates a new inventory listener. */
  public InventoryMenuListener(Plugin plugin) {
    this.plugin = plugin;
    inventoryMap = Maps.newHashMap();
  }

  /**
   * Creates a new {@link InventoryMenu}
   *
   * @param rows the number of rows the inventory should have
   * @param func a function that manipulates the {@link InventoryMenuBuilder} to design the {@link
   *     InventoryMenu}
   * @return the created {@link InventoryMenu}
   */
  public InventoryMenu createInventory(int rows, Consumer<InventoryMenuBuilder> func) {
    if (rows > 6) {
      throw new IllegalStateException("There can be at most 6 rows in an inventory");
    }

    InventoryMenuBuilder builder = new InventoryMenuBuilder(this, rows);
    func.accept(builder);
    return builder.build();
  }

  /**
   * Returns the {@link Plugin} associated with this inventory menu listener.
   *
   * @return the {@link Plugin} associated with this inventory menu listener.
   */
  public Plugin getPlugin() {
    return plugin;
  }

  /**
   * Links a {@link Inventory} and a {@link InventoryMenu} together for the listener to track
   *
   * @param bukkitInventory the bukkit inventory to form part of the link
   * @param inventory the inventory menu to form the other part of the link
   */
  void addInventory(Inventory bukkitInventory, InventoryMenu inventory) {
    inventoryMap.put(bukkitInventory, inventory);
  }

  /**
   * Checks to see whether a {@link Inventory} has a linked {@link InventoryMenu}
   *
   * @param inventory the bukkit inventory
   * @return true if there is a linked {@link InventoryMenu}, false otherwise
   */
  boolean hasInventory(Inventory inventory) {
    return inventoryMap.containsKey(inventory);
  }

  /**
   * Gets a linked {@link InventoryMenu} that is linked with a specified {@link Inventory}
   *
   * @param inventory the {@link Inventory} to look for a connection to
   * @return the connected {@link InventoryMenu}
   */
  InventoryMenu getInventory(Inventory inventory) {
    return inventoryMap.get(inventory);
  }

  /**
   * Removes {@link Inventory} and it's link from the linked mappings
   *
   * @param inventory the inventory to remove
   * @return the walrus inventory this inventory is linked to
   */
  InventoryMenu remove(Inventory inventory) {
    return inventoryMap.remove(inventory);
  }

  /**
   * Looks to see if a callback is executed when a player clicks on something inside of an {@link
   * Inventory}
   *
   * @param event the click event
   */
  @EventHandler
  public void onClick(InventoryClickEvent event) {
    Inventory inventory = event.getInventory();

    // player hasn't clicked an inventory, clicked outside, or didn't click any item
    if (inventory == null
        || event.getSlotType() == InventoryType.SlotType.OUTSIDE
        || InventoryUtils.isNothing(event.getCurrentItem())) {
      return;
    }

    if (hasInventory(inventory)) {
      int x = event.getSlot() % 9;
      int y = event.getSlot() / 9;
      event.setCancelled(true);
      getInventory(inventory).clickItem(x, y, event.getActor(), event.getClick());
    }
  }

  /**
   * Prevents players from moving items into {@link InventoryMenu}s
   *
   * @param event the move item event
   */
  @EventHandler
  public void onMove(InventoryMoveItemEvent event) {
    if (hasInventory(event.getDestination()) || hasInventory(event.getSource())) {
      event.setCancelled(true);
    }
  }

  /**
   * Removes a {@link Inventory} from the listener when a player stops viewing it.
   *
   * @param event the close inventory event
   */
  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    if (event.getInventory() == null) {
      return;
    }

    remove(event.getInventory());
  }
}
