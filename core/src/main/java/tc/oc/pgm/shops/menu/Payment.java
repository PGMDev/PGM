package tc.oc.pgm.shops.menu;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.inventory.Slot;
import tc.oc.pgm.util.material.Materials;

public class Payment {

  private final ChatColor color;
  private final Material currency;
  private final int price;

  private final @Nullable ItemStack item;

  public Payment(Material currency, int price, ChatColor color, @Nullable ItemStack item) {
    this.currency = currency;
    this.price = price;
    this.color = color;
    this.item = item;
  }

  public Material getCurrency() {
    return item != null ? item.getType() : currency;
  }

  public int getPrice() {
    return price;
  }

  public ChatColor getColor() {
    return color;
  }

  public @Nullable ItemStack getItem() {
    return item;
  }

  public boolean hasPayment(PlayerInventory inventory) {
    return getAffordableAmount(inventory, 1) > 0;
  }

  public boolean matches(ItemStack item) {
    return this.item != null
        ? Materials.itemsSimilar(item, this.item, true)
        : item.getType() == currency;
  }

  public int getAffordableAmount(PlayerInventory inventory, int max) {
    if (price <= 0) return max;

    int totalCurrency = 0;
    int targetCurrency = max * price; // total desired

    for (var slot : Slot.Storage.storage().toList()) {
      ItemStack item = slot.getItem(inventory);

      if (item != null && matches(item)) {
        totalCurrency += item.getAmount();

        if (totalCurrency >= targetCurrency) {
          return max;
        }
      }
    }

    return totalCurrency / price;
  }
}
