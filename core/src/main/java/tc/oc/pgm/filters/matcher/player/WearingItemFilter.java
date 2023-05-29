package tc.oc.pgm.filters.matcher.player;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.ApplyKitEvent;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;
import tc.oc.pgm.util.inventory.ItemMatcher;

public class WearingItemFilter extends ParticipantItemFilter {
  public WearingItemFilter(ItemMatcher matcher) {
    super(matcher);
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(
        InventoryClickEvent.class,
        PlayerItemTransferEvent.class,
        PlayerInteractEvent.class,
        ApplyKitEvent.class,
        PlayerItemBreakEvent.class);
  }

  @Override
  protected Stream<ItemStack> getItems(MatchPlayer player) {
    return Arrays.stream(player.getBukkit().getInventory().getArmorContents());
  }
}
