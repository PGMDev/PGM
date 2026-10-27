package tc.oc.pgm.vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.actions.MountAction;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.regions.RegionMatchModule;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;

/**
 * Looks after vehicles spawned by {@link MountAction}: applies their {@code dismount-filter}, keeps
 * other players out of them, and cleans them up when their rider gets out, dies, despawns or
 * leaves.
 */
@ListenerScope(MatchScope.RUNNING)
public class VehicleMatchModule implements MatchModule, Listener, Tickable {

  private final Match match;
  /** Players who may get out of their mount during the current tick, regardless of its filter. */
  private final Set<UUID> forcedExit = new HashSet<>();
  /**
   * A remove-filter imposed by a {@link #dismount} call, overriding the mount's own remove-filter
   * for the exit it triggers. Keyed by rider UUID, set only for the duration of {@code
   * leaveVehicle}.
   */
  private final Map<UUID, Filter> removeOverride = new HashMap<>();
  /**
   * The last location each rider's mount was allowed to occupy, used to put them back when they
   * cross into a region they may not enter. Keyed by rider UUID.
   */
  private final Map<UUID, Location> lastSafe = new HashMap<>();

  public VehicleMatchModule(Match match) {
    this.match = match;
  }

  /**
   * Enforce enter/leave region filters on riders. A living mount carries its rider without firing a
   * {@link org.bukkit.event.player.PlayerMoveEvent} or (for horses, pigs, ...) a {@link
   * org.bukkit.event.vehicle.VehicleMoveEvent}, so the usual move-based region checks never see the
   * rider. Each tick we re-check any rider that crossed a block boundary and, if denied, teleport
   * their mount back to its last allowed location. This matters most for mounts whose {@code
   * dismount-filter} keeps the rider stuck on board.
   */
  @Override
  public void tick(Match match, Tick tick) {
    // Each add is only meant to permit an exit during the tick it happens on
    forcedExit.clear();

    RegionMatchModule regions = match.getModule(RegionMatchModule.class);
    if (regions == null) {
      if (!lastSafe.isEmpty()) lastSafe.clear();
      return;
    }

    Set<UUID> riding = null;
    for (MatchPlayer player : match.getParticipants()) {
      Player bukkit = player.getBukkit();
      Entity vehicle = bukkit.getVehicle();
      if (MountAction.getMount(vehicle) == null) continue;

      UUID id = player.getId();
      if (riding == null) riding = new HashSet<>();
      riding.add(id);

      Location to = vehicle.getLocation();
      Location from = lastSafe.get(id);
      if (from == null || !from.getWorld().equals(to.getWorld())) {
        lastSafe.put(id, to);
        continue;
      }

      // Only re-query region filters when a block boundary is actually crossed
      if (from.getBlockX() == to.getBlockX()
          && from.getBlockY() == to.getBlockY()
          && from.getBlockZ() == to.getBlockZ()) {
        lastSafe.put(id, to);
        continue;
      }

      if (regions.denyVehicleMovement(player, from, to)) {
        Location reset = from.clone();
        reset.setYaw(to.getYaw());
        reset.setPitch(to.getPitch());
        // Teleporting a vehicle can eject its passenger on some versions, so re-seat if needed
        Entity passenger = vehicle.getPassenger();
        vehicle.teleport(reset);
        vehicle.setVelocity(new Vector());
        if (passenger != null && vehicle.getPassenger() == null) {
          vehicle.setPassenger(passenger);
        }
        // Leave lastSafe at the allowed location so repeated pushes keep getting blocked
      } else {
        lastSafe.put(id, to);
      }
    }

    if (riding == null) {
      if (!lastSafe.isEmpty()) lastSafe.clear();
    } else {
      lastSafe.keySet().retainAll(riding);
    }
  }

  /**
   * Take the player out of whatever they are riding, bypassing any dismount filter.
   *
   * @param removeFilter queried against the player once they are out, to decide whether the
   *     vehicle is removed too. Null uses the mount's {@code remove-filter}, and leaves vehicles
   *     that weren't spawned by a mount alone.
   */
  public void dismount(MatchPlayer player, @Nullable Filter removeFilter) {
    Player bukkit = player.getBukkit();
    Entity vehicle = bukkit.getVehicle();
    if (vehicle == null) return;
    boolean isMount = MountAction.getMount(vehicle) != null;

    allowExit(bukkit);
    // Impose the override before leaving, so onVehicleExit removes (or keeps) the mount per it
    // rather than falling back to the mount's own remove-filter
    if (removeFilter != null) removeOverride.put(bukkit.getUniqueId(), removeFilter);
    bukkit.leaveVehicle();
    removeOverride.remove(bukkit.getUniqueId());

    // onVehicleExit only manages mounts; remove any other vehicle here if explicitly asked to
    if (!isMount
        && removeFilter != null
        && !(vehicle instanceof Player)
        && removeFilter.query(player).isAllowed()) {
      vehicle.remove();
    }
  }

  private void allowExit(Player player) {
    forcedExit.add(player.getUniqueId());
  }

  private void releaseMount(Player player) {
    MatchPlayer matchPlayer = match.getPlayer(player);
    if (matchPlayer != null && MountAction.getMount(player.getVehicle()) != null) {
      dismount(matchPlayer, null);
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onVehicleExit(VehicleExitEvent event) {
    if (!(event.getExited() instanceof Player player)) return;
    Entity vehicle = event.getVehicle();
    MountAction mount = MountAction.getMount(vehicle);
    if (mount == null) return;

    MatchPlayer matchPlayer = match.getPlayer(player);
    boolean allowed = forcedExit.contains(player.getUniqueId())
        || matchPlayer == null
        || !matchPlayer.isAlive()
        || player.isDead()
        || mount.getDismountFilter().query(matchPlayer).isAllowed();

    if (!allowed) {
      event.setCancelled(true);
      return;
    }

    // Remove the abandoned vehicle once the player is out of it, if the remove-filter says so. A
    // dismount may override the mount's own remove-filter for this exit (see dismount()).
    Filter removeFilter = removeOverride.getOrDefault(player.getUniqueId(), mount.getRemoveFilter());
    if (matchPlayer != null && !removeFilter.query(matchPlayer).isAllowed()) return;
    PGM.get().getServer().getScheduler().runTask(PGM.get(), () -> {
      if (vehicle.isValid() && vehicle.getPassenger() == null) {
        MountAction.release(vehicle);
        vehicle.remove();
      }
    });
  }

  // A mount belongs to the player it was spawned for
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onVehicleEnter(VehicleEnterEvent event) {
    UUID owner = MountAction.getOwner(event.getVehicle());
    if (owner != null && !owner.equals(event.getEntered().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  // Teleports (respawns, teleport actions, match cycling) must be able to pull players out
  @EventHandler(priority = EventPriority.LOWEST)
  public void onTeleport(PlayerTeleportEvent event) {
    if (MountAction.getMount(event.getPlayer().getVehicle()) != null) {
      allowExit(event.getPlayer());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onDespawn(ParticipantDespawnEvent event) {
    releaseMount(event.getPlayer().getBukkit());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent event) {
    releaseMount(event.getPlayer());
  }
}
