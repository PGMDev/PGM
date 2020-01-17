package tc.oc.pgm.modules;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.filters.BlockFilter;
import tc.oc.pgm.kits.ArmorType;
import tc.oc.pgm.kits.KitMatchModule;

public class ItemKeepMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Set<BlockFilter> itemsToKeep;
  private final Set<BlockFilter> armorToKeep;
  private HashMap<MatchPlayer, Map<Integer, ItemStack>> keptInv = Maps.newHashMap();
  private HashMap<MatchPlayer, Map<ArmorType, ItemStack>> keptArmor = Maps.newHashMap();

  public ItemKeepMatchModule(
      Match match, Set<BlockFilter> itemsToKeep, Set<BlockFilter> armorToKeep) {
    this.match = match;
    this.itemsToKeep = itemsToKeep;
    this.armorToKeep = armorToKeep;
  }

  private boolean canKeepItem(ItemStack stack) {
    for (BlockFilter filter : this.itemsToKeep) {
      if (filter.matches(stack.getData())) return true;
    }
    return false;
  }

  private boolean canKeepArmor(ItemStack stack) {
    for (BlockFilter filter : this.itemsToKeep) {
      if (filter.matches(stack.getData())) return true;
    }
    for (BlockFilter filter : this.armorToKeep) {
      if (filter.matches(stack.getData())) return true;
    }
    return false;
  }

  /**
   * NOTE: Must be called before {@link
   * tc.oc.pgm.tracker.trackers.DeathTracker#onPlayerDeath(PlayerDeathEvent)}
   */
  @SuppressWarnings("deprecation")
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void processPlayerDeath(PlayerDeathEvent event) {
    MatchPlayer player = this.match.getPlayer(event.getEntity());
    if (player == null || !player.isParticipating()) {
      return;
    }

    ItemStack[] carrying = event.getEntity().getInventory().getContents();
    Map<Integer, ItemStack> keptItems = new HashMap<>();
    for (int slot = 0; slot < carrying.length; slot++) {
      ItemStack stack = carrying[slot];
      if (stack != null && this.canKeepItem(stack)) {
        event.getDrops().remove(stack);
        keptItems.put(slot, stack);
      }
    }

    if (!keptItems.isEmpty()) {
      this.keptInv.put(player, keptItems);
    }

    KitMatchModule kitMatchModule = this.match.getModule(KitMatchModule.class);
    ItemStack[] wearing = event.getEntity().getInventory().getArmorContents();
    Map<ArmorType, ItemStack> keptArmor = new HashMap<>();
    for (int slot = 0; slot < wearing.length; slot++) {
      ItemStack stack = wearing[slot];
      if (stack != null) {
        if (this.canKeepArmor(stack)) {
          event.getDrops().remove(stack);
          keptArmor.put(ArmorType.byArmorSlot(slot), stack);
        } else if (kitMatchModule != null
            && !"true".equalsIgnoreCase(this.match.getWorld().getGameRuleValue("keepInventory"))) {
          // TODO: When we have an improved player drops module, the drops will
          // be available on the PGMPlayerDeathEvent. KitMatchModule can listen
          // for that and deal with the locked armor itself.
          kitMatchModule.lockArmorSlot(player, ArmorType.byArmorSlot(slot), false);
        }
      }
    }

    if (!keptArmor.isEmpty()) {
      this.keptArmor.put(player, keptArmor);
    }
  }

  public void restoreKeptInventory(MatchPlayer player) {
    Map<Integer, ItemStack> kept = this.keptInv.remove(player);
    if (kept != null) {
      List<ItemStack> displaced = new ArrayList<>();
      PlayerInventory inv = player.getBukkit().getInventory();

      for (Map.Entry<Integer, ItemStack> entry : kept.entrySet()) {
        int slot = entry.getKey();
        ItemStack keptStack = entry.getValue();
        ItemStack invStack = inv.getItem(slot);

        if (invStack == null) {
          inv.setItem(slot, keptStack);
        } else {
          if (invStack.isSimilar(keptStack)) {
            int n =
                Math.min(keptStack.getAmount(), invStack.getMaxStackSize() - invStack.getAmount());
            invStack.setAmount(invStack.getAmount() + n);
            keptStack.setAmount(keptStack.getAmount() - n);
          }
          if (keptStack.getAmount() > 0) {
            displaced.add(keptStack);
          }
        }
      }

      for (ItemStack stack : displaced) {
        inv.addItem(stack);
      }
    }
  }

  public void restoreKeptArmor(MatchPlayer player) {
    Map<ArmorType, ItemStack> kept = this.keptArmor.remove(player);
    if (kept != null) {
      for (Map.Entry<ArmorType, ItemStack> entry : kept.entrySet()) {
        entry.getKey().setItem(player.getBukkit().getInventory(), entry.getValue());
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void partyChange(PlayerPartyChangeEvent event) {
    this.keptInv.remove(event.getPlayer());
    this.keptArmor.remove(event.getPlayer());
  }
}
