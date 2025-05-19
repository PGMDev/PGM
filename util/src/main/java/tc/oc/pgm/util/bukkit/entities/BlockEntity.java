package tc.oc.pgm.util.bukkit.entities;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Platform;

/**
 * Implements a wrapper for block-like entities, used for projectiles. Modern implementations will
 * use display entities, legacy (1.8) will use falling sand. <br>
 *
 * @implNote For this class, location is actually the center of the entity
 */
public interface BlockEntity {
  BlockEntity.Factory FACTORY = Platform.get(BlockEntity.Factory.class);

  static BlockEntity spawnBlockEntity(
      Location center, BlockMaterialData blockMaterialData, float size, Vector velocity) {
    return FACTORY.spawnBlockEntity(center, blockMaterialData, size, velocity);
  }

  void teleport(Location center);

  void remove();

  interface Factory {
    BlockEntity spawnBlockEntity(
        Location center, BlockMaterialData blockMaterialData, float size, Vector velocity);
  }
}
