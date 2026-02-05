package tc.oc.pgm.shops.menu;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.material.Materials;

public record Payment(
    Material currency, int price, ChatColor color, @Nullable ItemStack item) {

  @Override
  public Material currency() {
    return item != null ? item.getType() : currency;
  }

  @Deprecated
  public Material getCurrency() {
    return currency();
  }

  @Deprecated
  public int getPrice() {
    return price();
  }

  @Deprecated
  public ChatColor getColor() {
    return color();
  }

  @Deprecated
  public @Nullable ItemStack getItem() {
    return item();
  }

  public boolean hasPayment(PlayerInventory inventory) {
    if (price <= 0) return true;

    int remaining = price;
    for (ItemStack item : inventory.getContents()) {
      if (item == null || !matches(item)) continue;
      if ((remaining -= item.getAmount()) <= 0) return true;
    }
    return false;
  }

  public boolean matches(ItemStack item) {
    return this.item != null
        ? Materials.itemsSimilar(item, this.item, true)
        : item.getType() == currency;
  }
}
