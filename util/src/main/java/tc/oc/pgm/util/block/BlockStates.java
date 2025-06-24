package tc.oc.pgm.util.block;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;

public interface BlockStates {

  static BlockState toAir(BlockState state) {
    return toAir(state.getBlock());
  }

  static BlockState toAir(Block block) {
    BlockState newState = block.getState(); // this creates a new copy of the state
    MaterialData.AIR.applyTo(newState);
    return newState;
  }

  static BlockState cloneWithMaterial(Block block, Material material) {
    return cloneWithMaterial(block, MaterialData.block(material));
  }

  static BlockState cloneWithMaterial(Block block, BlockState blockState) {
    return cloneWithMaterial(block, MaterialData.block(blockState));
  }

  static BlockState cloneWithMaterial(Block block, BlockMaterialData materialData) {
    BlockState state = block.getState();
    materialData.applyTo(state);
    return state;
  }

  static BlockState create(World world, BlockVector pos, BlockMaterialData materialData) {
    BlockState state = pos.toLocation(world).getBlock().getState();
    materialData.applyTo(state);
    return state;
  }
}
