package tc.oc.pgm.util.block;

import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;

public interface BlockFaces {

  BlockFace[] NEIGHBORS = {
    BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN
  };

  BlockFace[] CLOCKWISE = new BlockFace[] {
    BlockFace.SOUTH,
    BlockFace.SOUTH_SOUTH_WEST,
    BlockFace.SOUTH_WEST,
    BlockFace.WEST_SOUTH_WEST,
    BlockFace.WEST,
    BlockFace.WEST_NORTH_WEST,
    BlockFace.NORTH_WEST,
    BlockFace.NORTH_NORTH_WEST,
    BlockFace.NORTH,
    BlockFace.NORTH_NORTH_EAST,
    BlockFace.NORTH_EAST,
    BlockFace.EAST_NORTH_EAST,
    BlockFace.EAST,
    BlockFace.EAST_SOUTH_EAST,
    BlockFace.SOUTH_EAST,
    BlockFace.SOUTH_SOUTH_EAST,
  };

  static BlockVector getRelative(BlockVector pos, BlockFace face) {
    return new BlockVector(
        pos.getBlockX() + face.getModX(),
        pos.getBlockY() + face.getModY(),
        pos.getBlockZ() + face.getModZ());
  }

  static BlockState getRelative(BlockState block, BlockFace face) {
    return block
        .getWorld()
        .getBlockAt(
            block.getX() + face.getModX(),
            block.getY() + face.getModY(),
            block.getZ() + face.getModZ())
        .getState();
  }

  static BlockFace yawToFace(float yaw) {
    return CLOCKWISE[Math.round(yaw / 22.5f) & 0xf];
  }

  static float faceToYaw(BlockFace face) {
    return switch (face) {
      case SOUTH_SOUTH_WEST -> 22.5f;
      case SOUTH_WEST -> 45f;
      case WEST_SOUTH_WEST -> 67.5f;
      case WEST -> 90f;
      case WEST_NORTH_WEST -> 112.5f;
      case NORTH_WEST -> 135f;
      case NORTH_NORTH_WEST -> 157.5f;
      case NORTH -> -180f;
      case NORTH_NORTH_EAST -> -157.5f;
      case NORTH_EAST -> -135f;
      case EAST_NORTH_EAST -> -112.5f;
      case EAST -> -90f;
      case EAST_SOUTH_EAST -> -67.5f;
      case SOUTH_EAST -> -45f;
      case SOUTH_SOUTH_EAST -> -22.5f;
      default -> 0f;
    };
  }
}
