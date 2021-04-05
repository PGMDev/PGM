package tc.oc.pgm.filters;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.ApplyKitEvent;

public class WearingItemFilter extends ParticipantItemFilter {
  public WearingItemFilter(ItemStack base) {
    super(base);
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(InventoryMoveItemEvent.class, ApplyKitEvent.class);
  }

  @Override
  protected ItemStack[] getItems(MatchPlayer player) {
    return player.getBukkit().getInventory().getArmorContents();
  }
}
