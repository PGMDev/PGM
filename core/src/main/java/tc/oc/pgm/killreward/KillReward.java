package tc.oc.pgm.killreward;

import com.google.common.collect.ImmutableList;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.kits.Kit;

public class KillReward {
  public final ImmutableList<ItemStack> items;
  public final Filter filter;
  public final Kit kit;

  public KillReward(ImmutableList<ItemStack> items, Filter filter, Kit kit) {
    this.items = items;
    this.filter = filter;
    this.kit = kit;
  }
}
