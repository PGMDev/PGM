package tc.oc.pgm.util.inventory;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public enum ArmorType {
  BOOTS,
  LEGGINGS,
  CHESTPLATE,
  HELMET;

  private static final int FIRST_ARMOR_SLOT = 36;

  public static ArmorType byArmorSlot(int slot) {
    return values()[slot];
  }

  public static ArmorType byInventorySlot(int slot) {
    return values()[slot - FIRST_ARMOR_SLOT];
  }

  public static boolean isArmorSlot(int inventorySlot) {
    return inventorySlot >= FIRST_ARMOR_SLOT && inventorySlot < FIRST_ARMOR_SLOT + values().length;
  }

  public int armorSlot() {
    return this.ordinal();
  }

  public int inventorySlot() {
    return FIRST_ARMOR_SLOT + this.ordinal();
  }

  public ItemStack getItem(PlayerInventory inv) {
    return inv.getItem(this.inventorySlot());
  }

  public void setItem(PlayerInventory inv, ItemStack stack) {
    inv.setItem(this.inventorySlot(), stack);
  }
}
