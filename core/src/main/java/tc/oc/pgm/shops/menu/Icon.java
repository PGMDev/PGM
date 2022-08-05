package tc.oc.pgm.shops.menu;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.kits.Kit;

public class Icon {

  private ImmutableList<Payment> payments;
  private final ItemStack item;
  private final Kit kit;

  public Icon(List<Payment> payments, ItemStack item, @Nullable Kit kit) {
    this.payments = ImmutableList.copyOf(payments);
    this.kit = kit;
    this.item = item;
  }

  public boolean isFree() {
    return payments.isEmpty() || payments.stream().anyMatch(p -> p.getPrice() < 1);
  }

  public ImmutableList<Payment> getPayments() {
    return payments;
  }

  public ItemStack getItem() {
    return item;
  }

  @Nullable
  public Kit getKit() {
    return kit;
  }
}
