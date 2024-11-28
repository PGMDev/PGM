package tc.oc.pgm.kits;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.kits.tag.Grenade;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.event.ItemTransferEvent;
import tc.oc.pgm.util.inventory.Slot;

@ListenerScope(MatchScope.RUNNING)
public class KitMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Set<KitRule> kitRules;

  public KitMatchModule(Match match, Set<KitRule> kitRules) {
    this.match = match;
    this.kitRules = kitRules;
  }

  @Override
  public void load() throws ModuleLoadException {
    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);

    for (KitRule kitRule : kitRules) {
      switch (kitRule.getAction()) {
        case GIVE:
          fmm.onRise(
              MatchPlayer.class,
              kitRule.getFilter(),
              player -> player.applyKit(kitRule.getKit(), true));
          break;
        case TAKE:
          fmm.onRise(MatchPlayer.class, kitRule.getFilter(), kitRule.getKit()::remove);
          break;
        case LEND:
          fmm.onChange(MatchPlayer.class, kitRule.getFilter(), (player, response) -> {
            if (response) {
              player.applyKit(kitRule.getKit(), true);
            } else {
              kitRule.getKit().remove(player);
            }
          });
          break;
      }
    }
  }

  private boolean isLocked(ItemStack item) {
    return item != null && ItemTags.LOCKED.has(item);
  }

  private boolean isUnshareable(ItemStack item) {
    return item != null && (isLocked(item) || ItemTags.PREVENT_SHARING.has(item));
  }

  private void sendLockWarning(HumanEntity player) {
    MatchPlayer matchPlayer = this.match.getPlayer(player);
    if (matchPlayer != null) {
      matchPlayer.sendWarning(translatable("match.item.locked"));
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onInventoryClick(final InventoryClickEvent event) {
    if (event instanceof InventoryCreativeEvent
        || event.getWhoClicked() != event.getInventory().getHolder()) {
      return;
    }

    // Break out of the switch if the action will move a locked item, otherwise return
    switch (event.getAction()) {
      case HOTBAR_SWAP:
      case HOTBAR_MOVE_AND_READD:
        Slot slot = Slot.Hotbar.forIndex(event.getHotbarButton());
        if (slot == null) return;
        ItemStack item = event.getWhoClicked().getInventory().getItem(slot.getIndex());
        if (item != null && ItemTags.LOCKED.has(item)) break;

      case PICKUP_ALL:
      case PICKUP_HALF:
      case PICKUP_SOME:
      case PICKUP_ONE:
      case SWAP_WITH_CURSOR:
      case MOVE_TO_OTHER_INVENTORY:
      case DROP_ONE_SLOT:
      case DROP_ALL_SLOT:
      case COLLECT_TO_CURSOR:
        if (ItemTags.LOCKED.has(event.getCurrentItem())) break;
      default:
        return;
    }

    event.setCancelled(true);
    sendLockWarning(event.getWhoClicked());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onGrenadeLaunch(final ProjectileLaunchEvent event) {
    if (event.getEntity().getShooter() instanceof Player) {
      Player player = (Player) event.getEntity().getShooter();
      ItemStack stack = player.getItemInHand();

      if (stack != null) {
        // special case for grenade arrows
        if (stack.getType() == Material.BOW) {
          int arrows = player.getInventory().first(Material.ARROW);
          if (arrows == -1) return;
          stack = player.getInventory().getItem(arrows);
        }

        Grenade grenade = Grenade.ITEM_TAG.get(stack);
        if (grenade != null) {
          grenade.set(PGM.get(), event.getEntity());
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onGrenadeExplode(final ProjectileHitEvent event) {
    if (event.getEntity().getShooter() instanceof Player) {
      Grenade grenade = Grenade.get(event.getEntity());
      if (grenade != null) {
        MISC_UTILS.createExplosion(event.getEntity(), grenade.power, grenade.fire, grenade.destroy);
        event.getEntity().remove();
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void playerDropItem(PlayerDropItemEvent event) {
    if (isLocked(event.getItemDrop().getItemStack())) {
      event.setCancelled(true);
      sendLockWarning(event.getPlayer());
    } else if (isUnshareable(event.getItemDrop().getItemStack())) {
      event.getItemDrop().remove();
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void checkItemTransfer(ItemTransferEvent event) {
    if (event.getReason() == ItemTransferEvent.Reason.PLACE && isUnshareable(event.getItem())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onDeath(PlayerDeathEvent event) {
    event.getDrops().removeIf(this::isUnshareable);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void checkInfiniteBlocks(BlockPlaceEvent event) {
    final ItemStack item = event.getPlayer().getItemInHand();
    if (ItemTags.INFINITE.has(item)) {
      // infinite block contains -1 items, giving -1 items sets the amount back to -1
      item.setAmount(ItemKit.INFINITE_STACK_SIZE);
    }
  }
}
