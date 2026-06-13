package tc.oc.pgm.entity;

import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Keeps a single frozen entity in place. The entity is never saved with its chunk, so owners should
 * respawn it once {@link #tick()} reports it as gone.
 */
@NullMarked
public final class FrozenEntity {

  private @Nullable Entity entity;
  private @Nullable Location location;

  /**
   * Spawn and freeze an entity at the given location, replacing any existing one.
   *
   * @return the spawned entity, or null if the location's chunk is not loaded
   */
  public @Nullable Entity spawn(Location location, Function<Location, Entity> spawner) {
    World world = location.getWorld();
    if (world == null
        || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
      return null;
    }

    remove();
    Entity entity = spawner.apply(location);
    NMS_HACKS.freezeEntity(entity);
    this.entity = entity;
    this.location = location;
    return entity;
  }

  /**
   * Hold the entity in place.
   *
   * @return false if there is no entity, or it is no longer valid (dead, removed or unloaded)
   */
  public boolean tick() {
    Entity entity = this.entity;
    Location location = this.location;
    if (entity == null || location == null) return false;

    if (!entity.isValid()) {
      clear();
      return false;
    }
    NMS_HACKS.tickFrozenEntity(entity, location);
    return true;
  }

  public void remove() {
    if (entity != null) entity.remove();
    clear();
  }

  private void clear() {
    this.entity = null;
    this.location = null;
  }

  public boolean isSpawned() {
    return entity != null;
  }

  public @Nullable Entity getEntity() {
    return entity;
  }

  public boolean is(Entity entity) {
    return entity.equals(this.entity);
  }
}
