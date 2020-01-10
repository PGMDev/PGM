package tc.oc.pgm.kits;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.ItemTransferEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerResetEvent;
import tc.oc.pgm.kits.tag.Grenade;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.world.NMSHacks;

@ListenerScope(MatchScope.RUNNING)
public class KitMatchModule implements MatchModule, Listener {

  private final Match match;
  private final SetMultimap<MatchPlayer, ArmorType> lockedArmorSlots = HashMultimap.create();

  public KitMatchModule(Match match) {
    this.match = match;
  }

  @Override
  public void disable() {
    this.lockedArmorSlots.clear();
  }

  public boolean lockArmorSlot(MatchPlayer player, ArmorType armorType, boolean locked) {
    if (locked) {
      return this.lockedArmorSlots.put(player, armorType);
    } else {
      return this.lockedArmorSlots.remove(player, armorType);
    }
  }

  /** Clear any {@link AttributeModifier}s applied to the player by {@link AttributeKit}s */
  public void clearAttributeModifiers(MatchPlayer player) {
    for (AttributeModifier modifier :
        match.getMapContext().legacy().getKits().getAttributeModifiers()) {
      // ASHCON: player.getBukkit().removeAttributeModifier(modifier);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(final PlayerQuitEvent event) {
    MatchPlayer player = this.match.getPlayer(event.getPlayer());
    if (player != null) {
      this.lockedArmorSlots.removeAll(player);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onInventoryClick(final InventoryClickEvent event) {
    if (event instanceof InventoryCreativeEvent
        || event.getWhoClicked() != event.getInventory().getHolder()
        || !ArmorType.isArmorSlot(event.getSlot())) {
      return;
    }

    MatchPlayer player = this.match.getPlayer((Player) event.getWhoClicked());
    if (player == null
        || !this.lockedArmorSlots.containsEntry(
            player, ArmorType.byInventorySlot(event.getSlot()))) {
      return;
    }

    switch (event.getAction()) {
      case PICKUP_ALL:
      case PICKUP_HALF:
      case PICKUP_SOME:
      case PICKUP_ONE:
      case SWAP_WITH_CURSOR:
      case MOVE_TO_OTHER_INVENTORY:
      case DROP_ONE_SLOT:
      case DROP_ALL_SLOT:
      case HOTBAR_SWAP:
      case HOTBAR_MOVE_AND_READD:
      case COLLECT_TO_CURSOR:
        event.setCancelled(true);
        player.sendWarning("This piece of armor cannot be removed", true);
        break;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onArmorBreak(final PlayerItemBreakEvent event) {
    MatchPlayer player = this.match.getPlayer(event.getPlayer());
    if (player == null) {
      return;
    }

    ItemStack[] armor = event.getPlayer().getInventory().getArmorContents();
    for (ArmorType armorType : ArmorType.values()) {
      int slot = armorType.ordinal();
      // Bukkit specifies the amount will be zero
      if (armor[slot] != null
          && armor[slot].getAmount() == 0
          && armor[slot].isSimilar(event.getBrokenItem())) {
        this.lockedArmorSlots.remove(player, armorType);
        break;
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerReset(final PlayerResetEvent event) {
    this.lockedArmorSlots.removeAll(event.getPlayer());
    clearAttributeModifiers(event.getPlayer());
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
        NMSHacks.createExplosion(
            event.getEntity(),
            event.getEntity().getLocation(),
            grenade.power,
            grenade.fire,
            grenade.destroy);
        event.getEntity().remove();
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void playerDropItem(PlayerDropItemEvent event) {
    if (ItemTags.PREVENT_SHARING.get(event.getItemDrop().getItemStack())) {
      event.getItemDrop().remove();
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void checkItemTransfer(ItemTransferEvent event) {
    if (event.getType() == ItemTransferEvent.Type.PLACE
        && ItemTags.PREVENT_SHARING.get(event.getItemStack())) {
      event.setCancelled(true);
    }
  }
}
