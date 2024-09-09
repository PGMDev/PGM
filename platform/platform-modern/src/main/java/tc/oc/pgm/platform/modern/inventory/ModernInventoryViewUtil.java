package tc.oc.pgm.platform.modern.inventory;

import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.bukkit.InventoryViewUtil;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = Supports.Variant.PAPER, minVersion = "1.20.6")
public class ModernInventoryViewUtil implements InventoryViewUtil {

  @Override
  public Inventory getTopInventory(InventoryView view) {
    return view.getTopInventory();
  }

  @Override
  public Inventory getBottomInventory(InventoryView view) {
    return view.getBottomInventory();
  }

  @Override
  public InventoryType getType(InventoryView view) {
    return view.getType();
  }

  @Override
  public void setItem(InventoryView view, int slot, ItemStack item) {
    view.setItem(slot, item);
  }

  @Override
  public ItemStack getItem(InventoryView view, int slot) {
    return view.getItem(slot);
  }

  @Override
  public void setCursor(InventoryView view, ItemStack cursor) {
    view.setCursor(cursor);
  }

  @Override
  public ItemStack getCursor(InventoryView view) {
    return view.getCursor();
  }

  @Override
  public int convertSlot(InventoryView view, int rawSlot) {
    return view.convertSlot(rawSlot);
  }

  @Override
  public int countSlots(InventoryView view) {
    return view.countSlots();
  }
}
