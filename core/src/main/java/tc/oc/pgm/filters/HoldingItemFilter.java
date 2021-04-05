package tc.oc.pgm.filters;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.ApplyKitEvent;

public class HoldingItemFilter extends ParticipantItemFilter {
  public HoldingItemFilter(ItemStack base) {
    super(base);
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(
        PlayerItemHeldEvent.class,
        PlayerPickupItemEvent.class,
        PlayerDropItemEvent.class,
        ApplyKitEvent.class,
        InventoryInteractEvent.class);
  }

  @Override
  protected ItemStack[] getItems(MatchPlayer player) {
    return new ItemStack[] {player.getBukkit().getItemInHand()};
  }
}
