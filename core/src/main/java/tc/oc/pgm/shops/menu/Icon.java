package tc.oc.pgm.shops.menu;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.kits.Kit;

public class Icon {

  private final ImmutableList<Payment> payments;
  private final ItemStack item;
  private final Filter filter;
  private final Kit kit;

  public Icon(List<Payment> payments, ItemStack item, Filter filter, @Nullable Kit kit) {
    this.payments = ImmutableList.copyOf(payments);
    this.item = item;
    this.filter = filter;
    this.kit = kit;
  }

  public boolean isFree() {
    return payments.isEmpty() || payments.stream().anyMatch(p -> p.getPrice() < 1);
  }

  public List<Payment> getPayments() {
    return payments;
  }

  public ItemStack getItem() {
    return item;
  }

  public Filter getFilter() {
    return filter;
  }

  @Nullable
  public Kit getKit() {
    return kit;
  }
}
