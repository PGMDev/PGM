package tc.oc.pgm.vehicle;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.actions.MountAction;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;

/**
 * Looks after vehicles spawned by {@link MountAction}: applies their {@code dismount-filter}, keeps
 * other players out of them, and cleans them up when their rider gets out, dies, despawns or
 * leaves.
 */
@ListenerScope(MatchScope.RUNNING)
public class VehicleMatchModule implements MatchModule, Listener {

  private final Match match;
  /** Players who may get out of their mount during the current tick, regardless of its filter. */
  private final Set<UUID> forcedExit = new HashSet<>();

  public VehicleMatchModule(Match match) {
    this.match = match;
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
    MountAction mount = MountAction.getMount(vehicle);

    allowExit(bukkit);
    bukkit.leaveVehicle();

    Filter filter = removeFilter != null
        ? removeFilter
        : mount != null ? mount.getRemoveFilter() : null;
    if (filter != null && !(vehicle instanceof Player) && filter.query(player).isAllowed()) {
      MountAction.release(vehicle);
      vehicle.remove();
    }
  }

  private void allowExit(Player player) {
    UUID id = player.getUniqueId();
    if (forcedExit.add(id)) {
      PGM.get().getServer().getScheduler().runTask(PGM.get(), () -> forcedExit.remove(id));
    }
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

    // Remove the abandoned vehicle once the player is out of it, if the mount says so
    if (matchPlayer != null && !mount.getRemoveFilter().query(matchPlayer).isAllowed()) return;
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
