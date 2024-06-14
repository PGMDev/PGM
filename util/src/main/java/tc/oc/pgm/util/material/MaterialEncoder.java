package tc.oc.pgm.util.material;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;

public interface MaterialEncoder {

  static int encodeMaterial(Block block) {
    return MaterialData.block(block).encoded();
  }

  static int encodeMaterial(BlockState block) {
    return MaterialData.block(block).encoded();
  }

  static int encodeMaterial(Location location) {
    return encodeMaterial(location.getBlock());
  }

  static int encodeMaterial(World world, BlockVector pos) {
    return encodeMaterial(world.getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()));
  }
}
