package tc.oc.pgm.shops.menu;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;

public class Icon implements Payable {

  private final ImmutableList<Payment> payments;

  @Getter
  private final ItemStack item;

  @Getter
  private final Filter filter;

  @Getter
  private final Action<? super MatchPlayer> action;

  public Icon(
      List<Payment> payments, ItemStack item, Filter filter, Action<? super MatchPlayer> action) {
    this.payments = ImmutableList.copyOf(payments);
    this.item = item;
    this.filter = filter;
    this.action = action;
  }

  public List<Payment> getPayments() {
    return payments;
  }
}
