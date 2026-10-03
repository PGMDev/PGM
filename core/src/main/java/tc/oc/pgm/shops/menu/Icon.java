package tc.oc.pgm.shops.menu;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

@FeatureInfo(name = "shop-item")
public class Icon extends SelfIdentifyingFeatureDefinition implements Payable {

  private final ImmutableList<Payment> payments;
  private final ItemStack item;
  private final Filter filter;
  private final Action<? super MatchPlayer> action;
  private final boolean stackable;

  public Icon(
      @Nullable String id,
      List<Payment> payments,
      ItemStack item,
      Filter filter,
      Action<? super MatchPlayer> action,
      boolean stackable) {
    super(id);
    this.payments = ImmutableList.copyOf(payments);
    this.item = item;
    this.filter = filter;
    this.action = action;
    this.stackable = stackable;
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

  public boolean isStackable() {
    return stackable;
  }
}
