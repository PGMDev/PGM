package tc.oc.pgm.util.block;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.MaterialDataProvider;

public interface BlockStates {

  static BlockState toAir(BlockState state) {
    return toAir(state.getBlock());
  }

  static BlockState toAir(Block block) {
    return MaterialDataProvider.from(Material.AIR).apply(block.getState());
  }

  static BlockState cloneWithMaterial(Block block, MaterialData material) {
    return material.apply(block.getState());
  }

  static BlockState cloneWithMaterial(Block block, Material material) {
    return MaterialDataProvider.from(material).apply(block.getState());
  }

  static BlockState create(World world, BlockVector pos, MaterialData materialData) {
    BlockState state = pos.toLocation(world).getBlock().getState();
    materialData.apply(state);
    return state;
  }

  static String format(BlockState state) {
    return "BlockState{pos=("
        + state.getX()
        + ", "
        + state.getY()
        + ", "
        + state.getZ()
        + ") world="
        + state
        + "}";
  }
}
