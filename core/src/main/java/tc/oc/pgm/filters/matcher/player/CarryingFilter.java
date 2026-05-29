package tc.oc.pgm.filters.matcher.player;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.inventory.InventoryUtils.INVENTORY_UTILS;

import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.ApplyKitEvent;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;
import tc.oc.pgm.util.inventory.ItemMatcher;
import tc.oc.pgm.util.inventory.Slot;
import tc.oc.pgm.util.inventory.SlotGroup;
import tc.oc.pgm.util.range.Ranges;

public class CarryingFilter extends ParticipantFilter {
  protected final ItemMatcher matcher;
  protected final int min, max;
  protected final SlotGroup slots;

  public CarryingFilter(
      ItemMatcher matcher, @Nullable Range<Integer> totalAmount, SlotGroup slots) {
    this.matcher = assertNotNull(matcher, "item");
    this.min = Ranges.optionalMinimum(totalAmount, 0);
    this.max = Ranges.optionalMaximum(totalAmount, Integer.MAX_VALUE);
    this.slots = assertNotNull(slots, "slots");
  }

  @Override
  public boolean isDynamic() {
    return true;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    var events = new ArrayList<Class<? extends Event>>();
    events.add(PlayerItemTransferEvent.class);
    events.add(PlayerItemBreakEvent.class);
    events.add(ApplyKitEvent.class);
    events.add(InventoryClickEvent.class);
    events.add(PlayerInteractEvent.class);
    boolean hands = slots.containsAny(SlotGroup.HANDS);
    boolean hotbar = hands || slots.containsAny(SlotGroup.HOTBAR);
    boolean storage = hands || hotbar || slots.containsAny(SlotGroup.STORAGE);
    if (hands) {
      events.add(PlayerItemHeldEvent.class);
    }
    if (hotbar) {
      events.add(PlayerItemConsumeEvent.class);
      events.add(PlayerEditBookEvent.class);
    }
    if (storage) {
      events.add(PlayerBucketFillEvent.class);
      events.add(PlayerBucketEmptyEvent.class);
      events.add(EntityShootBowEvent.class);
    }
    events.addAll(INVENTORY_UTILS.getRelevantEvents(slots));
    return Collections.unmodifiableList(events);
  }

  @Override
  public boolean matches(PlayerQuery query, MatchPlayer player) {
    // Fast-case when no range is defined
    if (min == 0 && max == Integer.MAX_VALUE)
      return slots.stream(player.getInventory()).anyMatch(matcher::matches);

    // There's always an early-exit condition, either for returning true or returning false.
    // If max is unset, if (found > (min - 1)) return true;
    // If max is set,   if (found > max) return false;
    // These two options are merged using earlyExit and exitThreshold.
    boolean earlyExit = max == Integer.MAX_VALUE;
    int exitThreshold = earlyExit ? min - 1 : max;

    var inv = player.getInventory();
    int found = 0;
    for (Slot.Player slot : slots) {
      var item = slot.getItem(inv);
      if (item != null && matcher.matches(item)) {
        found += item.getAmount();
        if (found > exitThreshold) return earlyExit;
      }
    }
    return found >= min;
  }
}
