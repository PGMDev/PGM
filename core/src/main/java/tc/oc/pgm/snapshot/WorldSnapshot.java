package tc.oc.pgm.snapshot;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.block.BlockData;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;

public class WorldSnapshot {
  private final World world;
  private final Map<ChunkVector, ChunkSnapshot> chunkSnapshots = new HashMap<>();
  private final BudgetWorldEdit worldEdit;

  public WorldSnapshot(World world) {
    this.world = world;
    this.worldEdit = new BudgetWorldEdit(world, this);
  }

  public BlockMaterialData getOriginalMaterial(Vector vector) {
    return getOriginalMaterial(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
  }

  public BlockMaterialData getOriginalMaterial(int x, int y, int z) {
    if (y < 0 || y >= 256) return MaterialData.AIR;

    ChunkVector chunkVector = ChunkVector.ofBlock(x, y, z);
    ChunkSnapshot chunkSnapshot = chunkSnapshots.get(chunkVector);
    if (chunkSnapshot != null) {
      return MaterialData.block(chunkSnapshot, chunkVector.worldToChunk(x, y, z));
    } else {
      return MaterialData.block(world.getBlockAt(x, y, z));
    }
  }

  public BlockState getOriginalBlock(int x, int y, int z) {
    BlockState state = world.getBlockAt(x, y, z).getState();
    if (y < 0 || y >= 256) return state;

    ChunkVector chunkVector = ChunkVector.ofBlock(x, y, z);
    ChunkSnapshot chunkSnapshot = chunkSnapshots.get(chunkVector);
    if (chunkSnapshot != null) {
      MaterialData.block(chunkSnapshot, chunkVector.worldToChunk(x, y, z)).applyTo(state);
    }
    return state;
  }

  /**
   * Manually save the initial state of a block to the snapshot.
   *
   * @param cv the chunk vector to save
   * @param oldState optional block state to write on the snapshot
   */
  public void saveSnapshot(ChunkVector cv, @Nullable BlockState oldState) {
    chunkSnapshots.computeIfAbsent(cv, vec -> {
      if (oldState == null) return vec.getChunk(world).getChunkSnapshot(false, false, false);

      // ChunkSnapshot will have the post-event state unless we revert
      Block block = oldState.getBlock();

      BlockMaterialData old = MaterialData.block(oldState);
      BlockMaterialData current = MaterialData.block(block);
      boolean isModified = !old.equals(current);
      if (isModified) old.applyTo(block, false);

      var snap = vec.getChunk(world).getChunkSnapshot(false, false, false);

      if (isModified) current.applyTo(block, false);

      return snap;
    });
  }

  public void saveRegion(Region region) {
    region.getChunkPositions().forEach(cv -> this.saveSnapshot(cv, null));
  }

  public void placeBlocks(Region region, BlockVector offset, boolean update) {
    worldEdit.placeBlocks(region, offset, update);
  }

  public void removeBlocks(Region region, BlockVector offset, boolean update) {
    worldEdit.removeBlocks(region, offset, update);
  }

  /**
   * Get the original material data for a {@code region}.
   *
   * @param region the region to get block states from
   */
  public Iterable<BlockData> getMaterials(Region region) {
    return () ->
        MaterialData.iterator(chunkSnapshots, region.getStatic(world).getBlockVectorIterator());
  }
}
