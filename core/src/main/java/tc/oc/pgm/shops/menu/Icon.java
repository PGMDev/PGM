package tc.oc.pgm.shops.menu;

import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.kits.Kit;

public class Icon {

  private final Material currency;
  private final int price;
  private final ItemStack item;
  private final Kit kit;

  public Icon(Material currency, int price, ItemStack item, Kit kit) {
    this.currency = currency;
    this.price = price;
    this.kit = kit;
    this.item = item;
  }

  public Material getCurrency() {
    return currency;
  }

  public int getPrice() {
    return price;
  }

  public ItemStack getItem() {
    return item;
  }

  @Nullable
  public Kit getKit() {
    return kit;
  }
}
