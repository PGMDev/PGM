package tc.oc.pgm.util.block;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

public interface BlockVectors {

  static BlockVector position(BlockState block) {
    return new BlockVector(block.getX(), block.getY(), block.getZ());
  }

  static BlockVector center(Vector blockPos) {
    return new BlockVector(
        blockPos.getBlockX() + 0.5, blockPos.getBlockY() + 0.5, blockPos.getBlockZ() + 0.5);
  }

  static Location center(Location location) {
    Location center = location.clone();
    center.setX(center.getBlockX() + 0.5);
    center.setY(center.getBlockY() + 0.5);
    center.setZ(center.getBlockZ() + 0.5);
    return center;
  }

  static Location center(Block block) {
    return center(block.getLocation());
  }

  static Location center(BlockState state) {
    return center(state.getLocation());
  }

  static boolean isInside(Vector point, Location blockLocation) {
    return blockLocation.getX() <= point.getX()
        && point.getX() <= blockLocation.getX() + 1
        && blockLocation.getY() <= point.getY()
        && point.getY() <= blockLocation.getY() + 1
        && blockLocation.getZ() <= point.getZ()
        && point.getZ() <= blockLocation.getZ() + 1;
  }

  static Block blockAt(World world, BlockVector vector) {
    return world.getBlockAt(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
  }

  /** BlockVector encoding API - pack a BlockVector into a single long */
  int SHIFT = 21;

  long MASK = ~(-1 << SHIFT);
  long SIGN_MASK = 1 << (SHIFT - 1);

  /** Decode a single component from the packed coordinates */
  static long unpack(long packed, int shift) {
    packed >>= shift;

    // Sign extension
    if ((packed & SIGN_MASK) == 0) {
      packed &= MASK;
    } else {
      packed |= ~MASK;
    }

    return packed;
  }

  static BlockVector decodePos(long encoded) {
    return new BlockVector(
        unpack(encoded, 0), unpack(encoded, SHIFT), unpack(encoded, SHIFT + SHIFT));
  }

  static final long ENCODED_NULL_POS = Long.MIN_VALUE;

  static long encodePos(long x, long y, long z) {
    return (x & MASK) | ((y & MASK) << SHIFT) | ((z & MASK) << (SHIFT + SHIFT));
  }

  static long encodePos(BlockVector vector) {
    return encodePos(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
  }

  static long encodePos(Block block) {
    return encodePos(block.getX(), block.getY(), block.getZ());
  }

  static long encodePos(BlockState block) {
    return encodePos(block.getX(), block.getY(), block.getZ());
  }

  static TLongSet encodePosSet(Collection<?> vectors) {
    TLongSet encoded = new TLongHashSet(vectors.size());
    for (Object o : vectors) {
      if (o instanceof BlockVector) {
        encoded.add(encodePos((BlockVector) o));
      }
    }
    return encoded;
  }

  /**
   * Return the encoded location neighboring the given location on the given side. Equivalent to
   * {@link Block#getRelative}.
   */
  static long neighborPos(long encoded, BlockFace face) {
    return encodePos(
        unpack(encoded, 0) + face.getModX(),
        unpack(encoded, SHIFT) + face.getModY(),
        unpack(encoded, SHIFT + SHIFT) + face.getModZ());
  }

  /**
   * Return the {@link Block} in the given {@link World}, at the given encoded location. This method
   * is more efficient than creating an intermediate {@link BlockVector}, and more convenient.
   */
  static Block blockAt(World world, long encoded) {
    return world.getBlockAt(
        (int) unpack(encoded, 0),
        (int) unpack(encoded, SHIFT),
        (int) unpack(encoded, SHIFT + SHIFT));
  }

  /** Block world that a player can stand on */
  static boolean isSupportive(Material type) {
    if (type.isOccluding()) {
      return true;
    }

    // blocks that aren't listed as occluding but can support a player
    if (type.isBlock()) {
      if (type.name().endsWith("STAIRS")) return true;
      if (type.name().endsWith("STEP")) return true;

      switch (type) {
        case CHEST:
        case ENDER_CHEST:
        case TRAPPED_CHEST:
        case HOPPER:
        case ANVIL:
        case BEACON:
        case ENCHANTMENT_TABLE:
        case CAULDRON:
        case DAYLIGHT_DETECTOR:
        case DAYLIGHT_DETECTOR_INVERTED:
        case GLASS:
        case STAINED_GLASS:
        case GLOWSTONE:
        case ICE:
        case LEAVES:
        case LEAVES_2:
        case PISTON_BASE:
        case PISTON_STICKY_BASE:
        case REDSTONE_BLOCK:
        case SOIL:
        case TNT:
        case BARRIER:
        case CARPET:
        case WATER_LILY:
        case CAKE_BLOCK:
        case SLIME_BLOCK:
          return true;
      }
    }

    return false;
  }
}
