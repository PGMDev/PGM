package tc.oc.pgm.util.block;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.material.MaterialData;

public interface BlockStates {

  static BlockState toAir(BlockState state) {
    return toAir(state.getBlock());
  }

  static BlockState toAir(Block block) {
    BlockState newState = block.getState(); // this creates a new copy of the state
    newState.setType(Material.AIR);
    newState.setRawData((byte) 0);
    return newState;
  }

  static BlockState cloneWithMaterial(Block block, Material material) {
    return cloneWithMaterial(block, material, (byte) 0);
  }

  static BlockState cloneWithMaterial(Block block, Material material, byte data) {
    BlockState state = block.getState();
    state.setType(material);
    state.setRawData(data);
    return state;
  }

  static BlockState cloneWithMaterial(Block block, BlockState blockState) {
    return cloneWithMaterial(block, MaterialData.from(blockState));
  }

  static BlockState cloneWithMaterial(Block block, org.bukkit.material.MaterialData materialData) {
    return cloneWithMaterial(block, MaterialData.from(materialData));
  }

  static BlockState cloneWithMaterial(Block block, MaterialData materialData) {
    BlockState state = block.getState();
    materialData.applyTo(state);
    return state;
  }

  static BlockState create(World world, BlockVector pos, MaterialData materialData) {
    BlockState state = pos.toLocation(world).getBlock().getState();
    materialData.applyTo(state);
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
        + state.getData()
        + "}";
  }
}
