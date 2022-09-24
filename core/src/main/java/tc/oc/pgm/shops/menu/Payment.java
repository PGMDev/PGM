package tc.oc.pgm.shops.menu;

import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
    return price <= 0
        || (item != null ? inventory.contains(item, price) : inventory.contains(currency, price));
  }

  public boolean matches(ItemStack item) {
    return this.item != null ? item.isSimilar(this.item, true) : item.getType() == currency;
  }
}
