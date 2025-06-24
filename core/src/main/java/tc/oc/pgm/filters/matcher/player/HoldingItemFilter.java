package tc.oc.pgm.filters.matcher.player;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
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
        PlayerItemBreakEvent.class,
        PlayerBucketFillEvent.class,
        PlayerBucketEmptyEvent.class,
        PlayerItemConsumeEvent.class,
        PlayerEditBookEvent.class);
  }

  @Override
  protected Stream<ItemStack> getItems(MatchPlayer player) {
    return Stream.of(player.getBukkit().getItemInHand());
  }
}
