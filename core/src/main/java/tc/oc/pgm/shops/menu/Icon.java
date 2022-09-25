package tc.oc.pgm.shops.menu;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;

public class Icon {

  private final ImmutableList<Payment> payments;
  private final ItemStack item;
  private final Filter filter;
  private final Action<? super MatchPlayer> action;

  public Icon(
      List<Payment> payments, ItemStack item, Filter filter, Action<? super MatchPlayer> action) {
    this.payments = ImmutableList.copyOf(payments);
    this.item = item;
    this.filter = filter;
    this.action = action;
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

  public Action<? super MatchPlayer> getAction() {
    return action;
  }
}
