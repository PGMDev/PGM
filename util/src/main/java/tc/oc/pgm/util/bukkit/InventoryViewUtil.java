package tc.oc.pgm.util.bukkit;

import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.platform.Platform;

/**
 * Modern versions of the game converted InventoryView from an abstract class to an interface. That
 * means trying to call any of the methods results in an IncompatibleClassChangeError. To
 * work-around it, any interaction with InventoryView should be thru these methods
 */
public interface InventoryViewUtil {
  InventoryViewUtil INVENTORY_VIEW = Platform.get(InventoryViewUtil.class);

  Inventory getTopInventory(InventoryView view);

  Inventory getBottomInventory(InventoryView view);

  InventoryType getType(InventoryView view);

  void setItem(InventoryView view, int slot, ItemStack item);

  ItemStack getItem(InventoryView view, int slot);

  void setCursor(InventoryView view, ItemStack cursor);

  ItemStack getCursor(InventoryView view);

  int convertSlot(InventoryView view, int rawSlot);

  int countSlots(InventoryView view);
}
