package tc.oc.pgm.util.block;

import static tc.oc.pgm.util.bukkit.BukkitUtils.parse;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collection;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.material.Materials;

public interface BlockVectors {

  // Names removed/renamed in higher versions
  Set<Material> SUPPORTIVE_BLOCKS = ImmutableSet.of(
      parse(Material::valueOf, "ENCHANTMENT_TABLE", "ENCHANTING_TABLE"),
      parse(Material::valueOf, "DAYLIGHT_DETECTOR_INVERTED", "DAYLIGHT_DETECTOR"),
      // TODO: PLATFORM DEPENDANT, new versions should use a longer list of blocks
      parse(Material::valueOf, "LEAVES", "LEGACY_LEAVES"),
      parse(Material::valueOf, "LEAVES_2", "LEGACY_LEAVES_2"),
      parse(Material::valueOf, "PISTON_BASE", "PISTON"),
      parse(Material::valueOf, "PISTON_STICKY_BASE", "STICKY_PISTON"),
      parse(Material::valueOf, "SOIL", "FARMLAND"),
      Materials.LILY_PAD,
      parse(Material::valueOf, "CAKE_BLOCK", "CAKE"));

  static BlockVector position(Block block) {
    return new BlockVector(block.getX(), block.getY(), block.getZ());
  }

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

  static LongSet encodePosSet(Collection<?> vectors) {
    LongSet encoded = new LongOpenHashSet(vectors.size());
    for (Object o : vectors) {
      if (o instanceof BlockVector vec) {
        encoded.add(encodePos(vec));
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
    return world.getBlockAt((int) unpack(encoded, 0), (int) unpack(encoded, SHIFT), (int)
        unpack(encoded, SHIFT + SHIFT));
  }

  // TODO: PLATFORM DEPENDANT, could optimize all the "endsWith" by using diff paths
  /** Block world that a player can stand on */
  static boolean isSupportive(Material type) {
    if (type.isOccluding()) {
      return true;
    }

    // blocks that aren't listed as occluding but can support a player
    if (type.isBlock()) {
      if (type.name().endsWith("STAIRS")) return true;
      if (type.name().endsWith("SLAB")) return true;
      if (type.name().endsWith("STEP")) return true;
      if (type.name().endsWith("STAINED_GLASS")) return true;
      if (type.name().endsWith("CARPET")) return true;
      if (type.name().endsWith("LEAVES")) return true;

      // Names removed/renamed in higher versions
      if (SUPPORTIVE_BLOCKS.contains(type)) return true;

      switch (type) {
        case CHEST:
        case ENDER_CHEST:
        case TRAPPED_CHEST:
        case HOPPER:
        case ANVIL:
        case BEACON:
        case CAULDRON:
        case DAYLIGHT_DETECTOR:
        case GLASS:
        case GLOWSTONE:
        case ICE:
        case REDSTONE_BLOCK:
        case TNT:
        case BARRIER:
        case SLIME_BLOCK:
          return true;
      }
    }

    return false;
  }
}
