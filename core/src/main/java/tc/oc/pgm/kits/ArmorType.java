package tc.oc.pgm.kits;

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

  public static ArmorType getArmorType(ItemStack itemStack) {
    switch (itemStack.getType()) {
      case DIAMOND_BOOTS:
      case IRON_BOOTS:
      case GOLD_BOOTS:
      case CHAINMAIL_BOOTS:
      case LEATHER_BOOTS:
        return BOOTS;
      case DIAMOND_LEGGINGS:
      case IRON_LEGGINGS:
      case GOLD_LEGGINGS:
      case CHAINMAIL_LEGGINGS:
      case LEATHER_LEGGINGS:
        return LEGGINGS;
      case DIAMOND_CHESTPLATE:
      case IRON_CHESTPLATE:
      case GOLD_CHESTPLATE:
      case CHAINMAIL_CHESTPLATE:
      case LEATHER_CHESTPLATE:
        return CHESTPLATE;
      case DIAMOND_HELMET:
      case IRON_HELMET:
      case GOLD_HELMET:
      case CHAINMAIL_HELMET:
      case LEATHER_HELMET:
        return HELMET;
      default:
        return null;
    }
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
