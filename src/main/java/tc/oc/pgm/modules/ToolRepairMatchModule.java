package tc.oc.pgm.modules;

import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.Sound;
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
    ItemStack pickup = event.getItem().getItemStack();

    int hitsLeft = pickup.getType().getMaxDurability() - pickup.getDurability() + 1;
    stack.setDurability((short) Math.max(stack.getDurability() - hitsLeft, 0));

    event.setCancelled(true);
    event.getItem().remove();
    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ITEM_PICKUP, 0.5f, 1f);
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
