package tc.oc.pgm.filters.matcher.player;

import static tc.oc.pgm.util.bukkit.InventoryViewUtil.INVENTORY_VIEW;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.ApplyKitEvent;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;
import tc.oc.pgm.util.inventory.ItemMatcher;

public class CarryingItemFilter extends ParticipantItemFilter {
  public CarryingItemFilter(ItemMatcher matcher) {
    super(matcher);
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(
        PlayerItemTransferEvent.class,
        ApplyKitEvent.class,
        PlayerItemBreakEvent.class,
        EntityShootBowEvent.class,
        PlayerBucketFillEvent.class,
        PlayerBucketEmptyEvent.class,
        PlayerItemConsumeEvent.class,
        PlayerEditBookEvent.class);
  }

  @Override
  protected Stream<ItemStack> getItems(MatchPlayer player) {
    Stream<ItemStack> inventory = Stream.concat(
        Arrays.stream(player.getBukkit().getInventory().getContents()),
        Stream.of(player.getBukkit().getItemOnCursor()));

    // Potentially add the crafting grid if that's the currently open inventory
    InventoryView invView = player.getBukkit().getOpenInventory();
    InventoryType type = INVENTORY_VIEW.getType(invView);
    if (type == InventoryType.CRAFTING || type == InventoryType.WORKBENCH) {
      return Stream.concat(
          inventory, Arrays.stream(INVENTORY_VIEW.getTopInventory(invView).getContents()));
    }
    return inventory;
  }
}
