package tc.oc.pgm.pickup;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.FilterMatchModule;

@NullMarked
@ListenerScope(MatchScope.LOADED)
public class PickupMatchModule implements MatchModule, Tickable, Listener {

  private final Match match;
  private final List<Pickup> pickups;

  public PickupMatchModule(Match match, List<Pickup> pickups) {
    this.match = match;
    this.pickups = pickups;
  }

  @Override
  public void load() {
    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);
    for (Pickup pickup : pickups) {
      fmm.onChange(
          Match.class,
          pickup.getDefinition().getSpawnFilter(),
          (m, allowed) -> pickup.setSpawnAllowed(allowed));
    }
  }

  @Override
  public void tick(Match match, Tick tick) {
    for (Pickup pickup : pickups) {
      pickup.tick(tick);
    }
  }

  @Override
  public void unload() {
    for (Pickup pickup : pickups) {
      pickup.unload();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMove(PlayerMoveEvent event) {
    Location from = event.getFrom();
    Location to = event.getTo();
    if (to == null || (from.getWorld() == to.getWorld() && from.toVector().equals(to.toVector())))
      return;

    checkPickups(event, from, to);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTeleport(PlayerTeleportEvent event) {
    Location to = event.getTo();
    if (to == null) return;

    checkPickups(event, to, to);
  }

  @EventHandler(ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    if (isPickup(event.getEntity())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onInteract(PlayerInteractEntityEvent event) {
    if (isPickup(event.getRightClicked())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onInteractAt(PlayerInteractAtEntityEvent event) {
    if (isPickup(event.getRightClicked())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
    if (isPickup(event.getRightClicked())) event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true)
  public void onVehicleEnter(VehicleEnterEvent event) {
    if (isPickup(event.getVehicle()) || isPickup(event.getEntered())) event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true)
  public void onCombust(EntityCombustEvent event) {
    if (isPickup(event.getEntity())) event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true)
  public void onDeath(EntityDeathEvent event) {
    Pickup pickup = getPickup(event.getEntity());
    if (pickup == null) return;

    event.setDroppedExp(0);
    event.getDrops().clear();
    pickup.reset();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onChunkUnload(ChunkUnloadEvent event) {
    for (Entity entity : event.getChunk().getEntities()) {
      if (isPickup(entity)) entity.remove();
    }
  }

  private boolean isPickup(Entity entity) {
    return getPickup(entity) != null;
  }

  private @Nullable Pickup getPickup(Entity entity) {
    for (Pickup pickup : pickups) {
      if (pickup.isEntity(entity)) return pickup;
    }
    return null;
  }

  private void checkPickups(PlayerMoveEvent event, Location from, Location to) {
    if (!match.isRunning()) return;

    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null) return;

    for (Pickup pickup : pickups) {
      if (pickup.canPickup(player, from, to)) {
        pickup.pickup(player);
      }
    }
  }
}
