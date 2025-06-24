package tc.oc.pgm.killreward;

import com.google.common.collect.ImmutableList;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;

public class KillReward {
  public final ImmutableList<ItemStack> items;
  public final Filter filter;
  public final Action<? super MatchPlayer> action;
  public final Action<? super MatchPlayer> victimAction;

  public KillReward(
      ImmutableList<ItemStack> items,
      Filter filter,
      Action<? super MatchPlayer> action,
      Action<? super MatchPlayer> victimAction) {
    this.items = items;
    this.filter = filter;
    this.action = action;
    this.victimAction = victimAction;
  }
}
