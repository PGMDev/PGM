package tc.oc.pgm.shops.menu;

import org.bukkit.Material;
import org.bukkit.inventory.PlayerInventory;

public class Payment {

  private Material currency;
  private int price;

  public Payment(Material currency, int price) {
    this.currency = currency;
    this.price = price;
  }

  public Material getCurrency() {
    return currency;
  }

  public int getPrice() {
    return price;
  }

  public boolean hasPayment(PlayerInventory inventory) {
    return inventory.contains(currency, price);
  }
}
