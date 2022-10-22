package tc.oc.pgm.util.chunk;

import java.util.Objects;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

/** Represents the position of a chunk and implements {@link #equals} and {@link #hashCode}. */
public class ChunkVector {
  private final int x, z;

  private ChunkVector(int x, int z) {
    this.x = x;
    this.z = z;
  }

  public static ChunkVector of(Chunk chunk) {
    return new ChunkVector(chunk.getX(), chunk.getZ());
  }

  public static ChunkVector of(int x, int z) {
    return new ChunkVector(x, z);
  }

  public static ChunkVector ofBlock(int x, int y, int z) {
    return new ChunkVector(x >> 4, z >> 4);
  }

  public static ChunkVector ofBlock(Vector pos) {
    return ofBlock(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
  }

  public static ChunkVector ofBlock(Block block) {
    return ofBlock(block.getX(), block.getY(), block.getZ());
  }

  public static ChunkVector ofBlock(BlockState block) {
    return ofBlock(block.getX(), block.getY(), block.getZ());
  }

  public int getChunkX() {
    return x;
  }

  public int getChunkZ() {
    return z;
  }

  public int getBlockMinX() {
    return x << 4;
  }

  public int getBlockMinZ() {
    return z << 4;
  }

  public BlockVector getBlockMin() {
    return new BlockVector(getBlockMinX(), 0, getBlockMinZ());
  }

  public BlockVector chunkToWorld(int x, int y, int z) {
    return new BlockVector(x + getBlockMinX(), y, z + getBlockMinZ());
  }

  public BlockVector chunkToWorld(Vector pos) {
    return chunkToWorld(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
  }

  public BlockVector worldToChunk(int x, int y, int z) {
    return new BlockVector(x - getBlockMinX(), y, z - getBlockMinZ());
  }

  public BlockVector worldToChunk(Vector pos) {
    return worldToChunk(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
  }

  public Chunk getChunk(World world) {
    return world.getChunkAt(x, z);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ChunkVector)) return false;
    ChunkVector other = (ChunkVector) o;
    return getChunkX() == other.getChunkX() && getChunkZ() == other.getChunkZ();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getChunkX(), getChunkZ());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + getChunkX() + ", " + getChunkZ() + "}";
  }
}
