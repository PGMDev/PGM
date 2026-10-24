package tc.oc.pgm.action.actions;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
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
import tc.oc.pgm.vehicle.VehicleMatchModule;

/**
 * Spawns a vehicle or rideable mob (boat, minecart, horse, pig...) at the player and seats them in
 * it.
 *
 * <pre>{@code <mount type="oak boat" dismount-filter="never" remove-on-exit="true"/>}</pre>
 *
 * <ul>
 *   <li>{@code dismount-filter}: queried against the player whenever they try to get out by
 *       themselves. Defaults to always allowing it; {@code never} keeps them in. Death, teleports,
 *       despawning, quitting and the {@code <dismount>} action always get them out.
 *   <li>{@code remove-on-exit}: remove the vehicle once its rider gets out (default true).
 * </ul>
 *
 * <p>Being an action, it can be used anywhere actions are: kits ({@code <action>}), triggers,
 * consumables, projectile hits, etc. Untriggering it (e.g. a kit being taken away) takes the player
 * out of the vehicle. Tameable mobs are tamed to the player, horses and pigs are saddled. Players
 * already riding a vehicle from this action are left alone; any other vehicle is swapped out.
 */
public class MountAction extends AbstractAction<MatchPlayer> {

  /** Metadata on spawned vehicles; value is the {@link MountAction} that spawned it. */
  public static final String METADATA_KEY = "pgm-mount";
  /** Metadata on spawned vehicles; value is the UUID of the player it was spawned for. */
  public static final String OWNER_KEY = "pgm-mount-owner";

  private final EntityType type;
  private final Filter dismountFilter;
  private final boolean removeOnExit;

  public MountAction(EntityType type, Filter dismountFilter, boolean removeOnExit) {
    super(MatchPlayer.class);
    this.type = type;
    this.dismountFilter = dismountFilter;
    this.removeOnExit = removeOnExit;
  }

  public EntityType getType() {
    return type;
  }

  public Filter getDismountFilter() {
    return dismountFilter;
  }

  public boolean isRemovedOnExit() {
    return removeOnExit;
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
    Entity vehicle = location.getWorld().spawnEntity(location, type);
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
    if (vehicles != null) vehicles.dismount(player, true);
  }

  /** Make a rideable mob controllable by its rider. */
  private static void prepare(Entity vehicle, Player rider) {
    if (vehicle instanceof Tameable tameable) {
      tameable.setTamed(true);
      tameable.setOwner(rider);
    }
    if (vehicle instanceof InventoryHolder holder
        && holder.getInventory() instanceof HorseInventory inventory) {
      inventory.setSaddle(new ItemStack(Material.SADDLE));
    }
    if (vehicle instanceof Pig pig) {
      pig.setSaddle(true);
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
