package tc.oc.pgm.util.bukkit.entities;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Platform;

public interface BlockEntity {
  BlockEntity.Factory FACTORY = Platform.get(BlockEntity.Factory.class);

  static BlockEntity spawnBlockEntity(
      Location loc, BlockMaterialData blockMaterialData, float size, Vector velocity) {
    return FACTORY.spawnBlockEntity(loc, blockMaterialData, size, velocity);
  }

  Location getLocation();

  void teleport(Location loc);

  void remove();

  interface Factory {
    BlockEntity spawnBlockEntity(
        Location loc, BlockMaterialData blockMaterialData, float size, Vector velocity);
  }
}
