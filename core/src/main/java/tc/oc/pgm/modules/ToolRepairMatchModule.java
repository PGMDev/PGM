package tc.oc.pgm.modules;

import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.nms.NMSHacks;

@ListenerScope(MatchScope.RUNNING)
public class ToolRepairMatchModule implements MatchModule, Listener {
  protected final Set<Material> toRepair;

  public ToolRepairMatchModule(Match match, Set<Material> toRepair) {
    this.toRepair = toRepair;
  }

  private boolean canRepair(ItemStack pickup, ItemStack invStack) {
    return invStack != null
        && invStack.getType().equals(pickup.getType())
        && invStack.getEnchantments().equals(pickup.getEnchantments());
  }

  private void doRepair(PlayerPickupItemEvent event, ItemStack stack) {
    Item item = event.getItem();
    ItemStack pickup = item.getItemStack();

    event.setCancelled(true);
    NMSHacks.fakePlayerItemPickup(event.getPlayer(), item);

    int hitsLeft = pickup.getType().getMaxDurability() - pickup.getDurability() + 1;
    stack.setDurability((short) Math.max(stack.getDurability() - hitsLeft, 0));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void processRepair(PlayerPickupItemEvent event) {
    ItemStack pickup = event.getItem().getItemStack();

    if (this.toRepair.contains(pickup.getType())) {
      PlayerInventory inv = event.getPlayer().getInventory();
      for (ItemStack invStack :
          Iterables.concat(
              Arrays.asList(inv.getContents()), Arrays.asList(inv.getArmorContents()))) {
        if (this.canRepair(pickup, invStack)) {
          this.doRepair(event, invStack);
          return;
        }
      }
    }
  }
}
