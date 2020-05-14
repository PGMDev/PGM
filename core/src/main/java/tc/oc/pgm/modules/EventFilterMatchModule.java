package tc.oc.pgm.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dispenser;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.event.AdventureModeInteractEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerBlockTransformEvent;
import tc.oc.pgm.kits.ArmorType;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

/**
 * Listens to many events at low priority and cancels them if the actor is not allowed to interact
 * with the world. Also cancels a few events that we just don't want ever.
 *
 * <p>Any functionality beyond that should be implemented in other modules. This module should be
 * kept simple.
 */
@ListenerScope(MatchScope.LOADED)
public class EventFilterMatchModule implements MatchModule, Listener {

  private final Match match;

  public EventFilterMatchModule(Match match) {
    this.match = match;
  }

  boolean cancel(Cancellable event, @Nullable MatchPlayer actor, @Nullable Component message) {
    match.getLogger().fine("Cancel " + event + " actor=" + actor);
    event.setCancelled(true);
    if (actor != null && message != null) {
      actor.sendWarning(message, true);
    }
    return true;
  }

  boolean cancel(
      Cancellable event,
      boolean cancel,
      World world,
      @Nullable MatchPlayer actor,
      @Nullable Component message) {
    if (cancel && match.getWorld().equals(world)) {
      return cancel(event, actor, message);
    } else {
      match.getLogger().fine("Allow  " + event + " actor=" + actor);
      return false;
    }
  }

  boolean cancel(Cancellable event, boolean cancel, World world) {
    return cancel(event, cancel, world, null, null);
  }

  boolean cancelAlways(Cancellable event, World world) {
    return cancel(event, true, world);
  }

  boolean cancelUnlessInteracting(Cancellable event, MatchPlayer player) {
    return cancel(event, !player.canInteract(), player.getBukkit().getWorld(), player, null);
  }

  boolean cancelUnlessInteracting(Cancellable event, Entity entity) {
    if (!(entity instanceof Player)) {
      return false;
    }

    return cancel(
        event,
        match.getParticipant(entity) == null,
        entity.getWorld(),
        match.getPlayer(entity),
        null);
  }

  boolean cancelUnlessInteracting(Cancellable event, MatchPlayerState player) {
    return cancel(
        event, !player.getParty().isParticipating(), player.getMatch().getWorld(), null, null);
  }

  ClickType convertClick(ClickType clickType, Player player) {
    if (clickType == ClickType.RIGHT && player.isSneaking()) {
      return ClickType.SHIFT_RIGHT;
    } else {
      return clickType;
    }
  }

  @Nullable
  ClickType convertClick(Action action, Player player) {
    switch (action) {
      case LEFT_CLICK_BLOCK:
      case LEFT_CLICK_AIR:
        return ClickType.LEFT;

      case RIGHT_CLICK_BLOCK:
      case RIGHT_CLICK_AIR:
        return convertClick(ClickType.RIGHT, player);

      default:
        return null;
    }
  }

  // -------------------------------------------------------------
  // -- Unconditionally cancelled events i.e. rejected features --
  // -------------------------------------------------------------

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onPortalCreate(final PortalCreateEvent event) {
    cancelAlways(event, event.getWorld());
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onWeatherChange(final WeatherChangeEvent event) {
    cancelAlways(event, event.getWorld());
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onBedEnter(final PlayerBedEnterEvent event) {
    cancel(
        event,
        true,
        event.getPlayer().getWorld(),
        match.getPlayer(event.getPlayer()),
        new PersonalizedTranslatable("match.disabled.bed"));
  }

  // ---------------------------
  // -- Player item/block use --
  // ---------------------------

  // This handler listens on HIGHEST so that other plugins get a chance
  // to handle observer clicks before we cancel them i.e. WorldEdit.
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInteract(final PlayerInteractEvent event) {
    if (cancelUnlessInteracting(event, event.getPlayer())) {
      // Allow the how-to book to be read
      if (event.getMaterial() == Material.WRITTEN_BOOK) {
        event.setUseItemInHand(Event.Result.ALLOW);
      }

      MatchPlayer player = match.getPlayer(event.getPlayer());
      if (player == null) return;

      ClickType clickType = convertClick(event.getAction(), event.getPlayer());
      if (clickType == null) return;

      match.callEvent(
          new ObserverInteractEvent(
              player, clickType, event.getClickedBlock(), null, event.getItem()));
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onShoot(final EntityShootBowEvent event) {
    // PlayerInteractEvent is fired on draw, this is fired on release. Need to cancel both.
    cancelUnlessInteracting(event, event.getEntity());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onDispense(BlockDispenseEvent event) {
    ItemStack item = event.getItem();
    ArmorType armorType = ArmorType.getArmorType(item);

    // If the dispenser is equipping a player with armor the velocity is 0
    if (event.getVelocity().length() != 0 || armorType == null) {
      return;
    }

    World world = event.getWorld();
    Block block = event.getBlock();
    Inventory inventory = ((org.bukkit.block.Dispenser) block.getState()).getInventory();

    BlockFace facing = ((Dispenser) block.getState().getMaterialData()).getFacing();

    Location dispenseLocation =
        new Location(
            event.getWorld(),
            block.getX() + 0.5D + 0.7D * facing.getModX(),
            block.getY() + 1.2D * facing.getModY(),
            block.getZ() + 0.5D + 0.7D * facing.getModZ());

    Collection<Player> nearbyPlayers = world.getNearbyPlayers(dispenseLocation, 1);

    List<Player> nearbyParticipatingPlayers = new ArrayList<>();

    boolean interferingPlayerPresent = false;

    for (Player player : nearbyPlayers) {
      MatchPlayer matchPlayer = match.getPlayer(player);
      if (matchPlayer.isAlive()) {
        nearbyParticipatingPlayers.add(player);
      } else {
        interferingPlayerPresent = true;
      }
    }

    if (!interferingPlayerPresent) return;

    event.setCancelled(true);
    world.playEffect(dispenseLocation, Effect.CLICK1, 1, 16);

    boolean dropItem = true;
    for (Player player : nearbyParticipatingPlayers) {
      ItemStack currentItem = armorType.getItem(player.getInventory());
      if (currentItem == null || currentItem.getType() == Material.AIR) {
        armorType.setItem(player.getInventory(), item);
        dropItem = false;
        break;
      }
    }

    // copied from nms
    if (dropItem) {
      Item droppedItem = world.dropItem(dispenseLocation, item);
      Vector droppedItemVelocity = new Vector();
      Random random = match.getRandom();
      double baseSpeed = random.nextDouble() * 0.1D + 0.2D;
      double randomModifier = 0.007499999832361937D * 6D;
      droppedItemVelocity.setX((double) facing.getModX() * baseSpeed);
      droppedItemVelocity.setX(
          (double) facing.getModX() * baseSpeed + random.nextGaussian() * randomModifier);
      droppedItemVelocity.setY(0.20000000298023224D + random.nextGaussian() * randomModifier);
      droppedItemVelocity.setZ((double) facing.getModZ() * baseSpeed);
      droppedItemVelocity.setZ(
          (double) facing.getModZ() * baseSpeed + random.nextGaussian() * randomModifier);
      droppedItem.setVelocity(droppedItemVelocity);
    }

    // When this is called the amount for the armor being dispensed will be 0, we have to manually
    // set its size to less than 0 due to how the nms code works
    for (int i = 0; i < inventory.getSize(); i++) {
      ItemStack itemStack = inventory.getItem(i);
      if (itemStack != null && itemStack.getAmount() == 0) {
        itemStack.setAmount(-1);
        return;
      }
    }

    // If we reach here the armor selected was in a stack of more than 1 so we just remove from
    // the first similar stack
    for (int i = 0; i < inventory.getSize(); i++) {
      ItemStack itemStack = inventory.getItem(i);
      if (itemStack != null && itemStack.isSimilar(item)) {
        itemStack.subtract();
        return;
      }
    }
  }

  // --------------------------------------
  // -- Player interaction with entities --
  // --------------------------------------

  void callObserverInteractEvent(PlayerInteractEntityEvent event) {
    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null) return;

    match.callEvent(
        new ObserverInteractEvent(
            player,
            convertClick(ClickType.RIGHT, event.getPlayer()),
            null,
            event.getRightClicked(),
            event.getPlayer().getItemInHand()));
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onEntityInteract(final PlayerInteractEntityEvent event) {
    if (cancelUnlessInteracting(event, event.getPlayer())) {
      callObserverInteractEvent(event);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onArmorStandInteract(final PlayerInteractAtEntityEvent event) {
    cancelUnlessInteracting(event, event.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onArmorStandInteract(final PlayerArmorStandManipulateEvent event) {
    cancelUnlessInteracting(event, event.getPlayer());
  }

  // --------------------------------------
  // -- Player interaction with vehicles --
  // --------------------------------------

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onVehicleDamage(final VehicleDamageEvent event) {
    cancelUnlessInteracting(event, event.getAttacker());
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onVehiclePush(final VehicleEntityCollisionEvent event) {
    cancelUnlessInteracting(event, event.getEntity());
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onVehicleEnter(final VehicleEnterEvent event) {
    cancelUnlessInteracting(event, event.getEntered());
  }

  // ------------------------------------
  // -- Player interaction with blocks --
  // ------------------------------------

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onPlayerBlockChange(final PlayerBlockTransformEvent event) {
    cancelUnlessInteracting(event, event.getPlayerState());

    if (!event.isCancelled() && event.getNewState().getType() == Material.ENDER_CHEST) {
      cancel(
          event,
          true,
          event.getWorld(),
          event.getPlayer(),
          new PersonalizedTranslatable("match.disabled.enderChest"));
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onPlayerBlockDamage(final BlockDamageEvent event) {
    cancelUnlessInteracting(event, event.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onHangingBreak(final HangingBreakEvent event) {
    cancelUnlessInteracting(
        event,
        event instanceof HangingBreakByEntityEvent
            ? ((HangingBreakByEntityEvent) event).getRemover()
            : null);
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onAdventureModeInteract(final AdventureModeInteractEvent event) {
    cancelUnlessInteracting(event, event.getActor());
  }

  // --------------------------
  // -- Player damage/combat --
  // --------------------------

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onDamage(final EntityDamageEvent event) {
    cancelUnlessInteracting(event, event.getEntity());
    if (event instanceof EntityDamageByEntityEvent) {
      EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent) event;
      if (cancelUnlessInteracting(event, entityEvent.getDamager())) {
        MatchPlayer player = match.getPlayer(entityEvent.getDamager());
        if (player == null) return;

        match.callEvent(
            new ObserverInteractEvent(
                player,
                ClickType.LEFT,
                null,
                event.getEntity(),
                player.getInventory().getItemInHand()));
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onCombust(final EntityCombustEvent event) {
    cancelUnlessInteracting(event, event.getEntity());
    if (event instanceof EntityCombustByEntityEvent) {
      cancelUnlessInteracting(event, ((EntityCombustByEntityEvent) event).getCombuster());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onPotionSplash(final PotionSplashEvent event) {
    for (LivingEntity entity : event.getAffectedEntities()) {
      if (entity instanceof Player && match.getParticipant(entity) == null) {
        event.setIntensity(entity, 0);
      }
    }
  }

  // -----------------------------------
  // -- Player item/inventory actions --
  // -----------------------------------

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onPlayerDropItem(final PlayerDropItemEvent event) {
    if (match.getParticipant(event.getPlayer()) == null) {
      event.getItemDrop().remove();
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onPlayerPickupItem(final PlayerPickupItemEvent event) {
    cancelUnlessInteracting(event, event.getPlayer());
  }

  // ----------------------
  // -- Player targeting --
  // ----------------------

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onEntityTrack(final EntityTargetEvent event) {
    // Handles mobs and XP orbs
    if (event.getTarget() != null) cancelUnlessInteracting(event, event.getTarget());
  }
}
