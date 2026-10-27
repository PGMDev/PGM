package tc.oc.pgm.action.actions;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.entity.SpawnableEntity;
import tc.oc.pgm.vehicle.VehicleMatchModule;

/**
 * Spawns an entity at the player and seats them on it, using the same entity definition spawners
 * use, so any of its properties (and a kit, for mobs) can be set. Non-living vehicles like boats and
 * minecarts are allowed too.
 *
 * <p>{@code dismount-filter} is queried whenever the player tries to get out by themselves (default:
 * always); death, teleports, despawning, quitting and {@code <dismount>} always get them out. {@code
 * remove-filter} is queried once they are out, to decide whether the entity is removed (default:
 * always). Tamed mobs are owned by the rider. Players already riding an entity from this action are
 * left alone; anything else they're riding is swapped out.
 */
public class MountAction extends AbstractAction<MatchPlayer> {

  /** Metadata on spawned vehicles; value is the {@link MountAction} that spawned it. */
  public static final String METADATA_KEY = "pgm-mount";
  /** Metadata on spawned vehicles; value is the UUID of the player it was spawned for. */
  public static final String OWNER_KEY = "pgm-mount-owner";

  private final SpawnableEntity entity;
  private final Filter dismountFilter;
  private final Filter removeFilter;

  public MountAction(SpawnableEntity entity, Filter dismountFilter, Filter removeFilter) {
    super(MatchPlayer.class);
    this.entity = entity;
    this.dismountFilter = dismountFilter;
    this.removeFilter = removeFilter;
  }

  public Filter getDismountFilter() {
    return dismountFilter;
  }

  public Filter getRemoveFilter() {
    return removeFilter;
  }

  @Override
  public void trigger(MatchPlayer player, ActionContext context) {
    Player bukkit = player.getBukkit();
    if (getMount(bukkit.getVehicle()) == this) return;

    VehicleMatchModule vehicles = player.getMatch().needModule(VehicleMatchModule.class);
    if (bukkit.isInsideVehicle()) vehicles.dismount(player, null);

    Location location = bukkit.getLocation();
    Entity vehicle = entity.spawn(location);
    vehicle.setMetadata(METADATA_KEY, new FixedMetadataValue(PGM.get(), this));
    vehicle.setMetadata(OWNER_KEY, new FixedMetadataValue(PGM.get(), bukkit.getUniqueId()));
    prepare(vehicle, bukkit);

    // Seat them a tick later so the client has caught up with any spawn teleport
    UUID owner = bukkit.getUniqueId();
    Bukkit.getScheduler().runTask(PGM.get(), () -> {
      Player p = Bukkit.getPlayer(owner);
      if (p == null || !vehicle.isValid() || !p.getWorld().equals(vehicle.getWorld())) {
        vehicle.remove();
        return;
      }
      if (p.isInsideVehicle()) p.leaveVehicle();
      vehicle.setPassenger(p);
    });
  }

  /** Make a tamed mob owned by its rider, so it can be controlled. */
  private static void prepare(Entity vehicle, Player rider) {
    if (vehicle instanceof Tameable tameable && tameable.isTamed()) {
      tameable.setOwner(rider);
    }
  }

  /** The mount action that spawned this entity, or null if it wasn't spawned by one. */
  public static @Nullable MountAction getMount(@Nullable Entity entity) {
    if (entity == null) return null;
    for (MetadataValue value : entity.getMetadata(METADATA_KEY)) {
      if (value.getOwningPlugin() == PGM.get() && value.value() instanceof MountAction mount) {
        return mount;
      }
    }
    return null;
  }

  /** The player this entity was spawned for by a mount action, or null. */
  public static @Nullable UUID getOwner(@Nullable Entity entity) {
    if (entity == null) return null;
    for (MetadataValue value : entity.getMetadata(OWNER_KEY)) {
      if (value.getOwningPlugin() == PGM.get() && value.value() instanceof UUID uuid) return uuid;
    }
    return null;
  }

  /** Stop treating this entity as a mount, e.g. right before removing it. */
  public static void release(Entity entity) {
    entity.removeMetadata(METADATA_KEY, PGM.get());
    entity.removeMetadata(OWNER_KEY, PGM.get());
  }
}
