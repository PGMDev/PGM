package tc.oc.pgm.filters.matcher.player;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.ApplyKitEvent;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;
import tc.oc.pgm.util.inventory.ItemMatcher;

public class HoldingItemFilter extends ParticipantItemFilter {
  public HoldingItemFilter(ItemMatcher matcher) {
    super(matcher);
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(
        PlayerItemHeldEvent.class,
        PlayerItemTransferEvent.class,
        ApplyKitEvent.class,
        PlayerItemBreakEvent.class);
  }

  @Override
  protected ItemStack[] getItems(MatchPlayer player) {
    return new ItemStack[] {player.getBukkit().getItemInHand()};
  }
}
