package tc.oc.pgm.util.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public interface BlockStates {

  static BlockState toAir(BlockState state) {
    return toAir(state.getBlock());
  }

  static BlockState toAir(Block block) {
    BlockState newState = block.getState(); // this creates a new copy of the state
    newState.setType(Material.AIR);
    //    newState.setRawData((byte) 0);
    return newState;
  }

  static BlockState cloneWithMaterial(Block block, Material material) {
    BlockState state = block.getState();
    state.setType(material);
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
        + state.getType()
        + "}";
  }
}
