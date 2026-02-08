package tc.oc.pgm.modules;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;
import tc.oc.pgm.util.inventory.Slot;
import tc.oc.pgm.util.material.MaterialMatcher;

@ListenerScope(MatchScope.RUNNING)
public class ItemKeepMatchModule implements MatchModule, Listener {

  private final Match match;
  private final MaterialMatcher itemsToKeep;
  private final MaterialMatcher armorToKeep;
  private final Map<MatchPlayer, Map<Slot.Player, ItemStack>> keptInv = Maps.newHashMap();

  public ItemKeepMatchModule(
      Match match, MaterialMatcher itemsToKeep, MaterialMatcher armorToKeep) {
    this.match = match;
    this.itemsToKeep = itemsToKeep;
    this.armorToKeep = armorToKeep;
  }

  private boolean canKeepItem(Slot.Player slot, ItemStack stack) {
    return itemsToKeep.matches(stack) || (slot instanceof Slot.Armor && armorToKeep.matches(stack));
  }

  /**
   * NOTE: Must be called before
   * {@link tc.oc.pgm.tracker.trackers.DeathTracker#onPlayerDeath(PlayerDeathEvent)}
   */
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void processPlayerDeath(PlayerDeathEvent event) {
    MatchPlayer player = this.match.getPlayer(event.getEntity());
    if (player == null || !player.isParticipating()) {
      return;
    }

    var inv = event.getEntity().getInventory();
    Map<Slot.Player, ItemStack> keptItems = new HashMap<>();
    Slot.Player.forEach(inv, (slot, stack) -> {
      if (this.canKeepItem(slot, stack)) {
        event.getDrops().remove(stack);
        keptItems.put(slot, stack);
      }
    });
    if (!keptItems.isEmpty()) this.keptInv.put(player, keptItems);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void restoreKeptInventory(ParticipantSpawnEvent event) {
    var player = event.getPlayer();
    var kept = this.keptInv.remove(player);
    if (kept == null) return;
    List<ItemStack> displaced = new ArrayList<>();
    PlayerInventory inv = player.getBukkit().getInventory();

    for (var entry : kept.entrySet()) {
      var slot = entry.getKey();
      var keptStack = entry.getValue();
      var invStack = slot.getItem(inv);

      if (invStack == null || slot instanceof Slot.Armor) {
        slot.setItem(inv, keptStack);
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

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void partyChange(PlayerPartyChangeEvent event) {
    this.keptInv.remove(event.getPlayer());
  }
}
