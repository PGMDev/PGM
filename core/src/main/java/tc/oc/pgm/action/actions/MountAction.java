package tc.oc.pgm.action.actions;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
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
 * Spawns an entity at the player and seats them on it. The entity is a spawnable entity, the same
 * definition spawners use, so any of its properties (and a kit, for mobs) can be set. Non-living
 * vehicles like boats and minecarts are allowed too.
 *
 * <pre>{@code
 * <mount dismount-filter="never" remove-filter="always">
 *   <entity type="horse" tamed="true" jump-strength="1" kit="horse-armor"/>
 * </mount>
 * }</pre>
 *
 * <ul>
 *   <li>{@code dismount-filter}: queried against the player whenever they try to get out by
 *       themselves (default: always). Death, teleports, despawning, quitting and {@code
 *       <dismount>} always get them out.
 *   <li>{@code remove-filter}: queried against the player once they are out, to decide whether
 *       the entity is removed (default: always).
 * </ul>
 *
 * <p>Being an action it works anywhere actions do, and untriggering it (e.g. a lent kit being
 * removed) takes the player out. Tamed mobs are owned by the rider and horses are saddled, so
 * they can be steered. Players already riding an entity from this action are left alone; anything
 * else they're riding is swapped out.
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

    VehicleMatchModule vehicles = player.getMatch().getModule(VehicleMatchModule.class);
    if (bukkit.isInsideVehicle()) {
      if (vehicles != null) vehicles.dismount(player, null);
      else bukkit.leaveVehicle();
    }

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

  @Override
  public void untrigger(MatchPlayer player, ActionContext context) {
    if (getMount(player.getBukkit().getVehicle()) != this) return;
    VehicleMatchModule vehicles = player.getMatch().getModule(VehicleMatchModule.class);
    if (vehicles != null) vehicles.dismount(player, null);
  }

  /** Make a rideable mob controllable by its rider. */
  private static void prepare(Entity vehicle, Player rider) {
    if (vehicle instanceof Tameable tameable && tameable.isTamed()) {
      tameable.setOwner(rider);
    }
    if (vehicle instanceof InventoryHolder holder
        && holder.getInventory() instanceof HorseInventory inventory
        && inventory.getSaddle() == null) {
      inventory.setSaddle(new ItemStack(Material.SADDLE));
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
