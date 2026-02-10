package tc.oc.pgm.killreward;

import com.google.common.collect.ImmutableList;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;

public record KillReward(
    ImmutableList<ItemStack> items,
    Filter filter,
    Action<? super MatchPlayer> action,
    Action<? super MatchPlayer> victimAction) {}
