package tc.oc.pgm.snapshot;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.block.BlockData;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.nms.NMSHacks;

public class WorldSnapshot {
  private final World world;
  private final Map<ChunkVector, ChunkSnapshot> chunkSnapshots = new HashMap<>();
  private final BudgetWorldEdit worldEdit;

  public WorldSnapshot(World world) {
    this.world = world;
    this.worldEdit = new BudgetWorldEdit(world, this);
  }

  public MaterialData getOriginalMaterial(Vector vector) {
    return getOriginalMaterial(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
  }

  public MaterialData getOriginalMaterial(int x, int y, int z) {
    if (y < 0 || y >= 256) return MaterialData.from(Material.AIR);

    ChunkVector chunkVector = ChunkVector.ofBlock(x, y, z);
    ChunkSnapshot chunkSnapshot = chunkSnapshots.get(chunkVector);
    if (chunkSnapshot != null) {
      return MaterialData.from(chunkSnapshot, chunkVector.worldToChunk(x, y, z));
    } else {
      return MaterialData.from(world.getBlockAt(x, y, z));
    }
  }

  public BlockState getOriginalBlock(int x, int y, int z) {
    BlockState state = world.getBlockAt(x, y, z).getState();
    if (y < 0 || y >= 256) return state;

    ChunkVector chunkVector = ChunkVector.ofBlock(x, y, z);
    ChunkSnapshot chunkSnapshot = chunkSnapshots.get(chunkVector);
    if (chunkSnapshot != null) {
      MaterialData.from(chunkSnapshot, chunkVector.worldToChunk(x, y, z)).applyTo(state);
    }
    return state;
  }

  /**
   * Manually save the initial state of a block to the snapshot.
   *
   * @param cv the chunk vector to save
   * @param state optional block state to write on the snapshot
   */
  public void saveSnapshot(ChunkVector cv, @Nullable BlockState state) {
    if (!chunkSnapshots.containsKey(cv)) {
      ChunkSnapshot snapshot = cv.getChunk(world).getChunkSnapshot();

      // ChunkSnapshot is very likely to have the post-event state already,
      // so we have to correct it
      if (state != null) NMSHacks.updateChunkSnapshot(snapshot, state);

      chunkSnapshots.put(cv, snapshot);
    }
  }

  public void saveRegion(Region region) {
    region.getChunkPositions().forEach(cv -> this.saveSnapshot(cv, null));
  }

  public void placeBlocks(Region region, BlockVector offset) {
    worldEdit.placeBlocks(region, offset);
  }

  public void removeBlocks(Region region, BlockVector offset) {
    worldEdit.removeBlocks(region, offset);
  }

  /**
   * Get the original material data for a {@code region}.
   *
   * @param region the region to get block states from
   */
  public Iterable<BlockData> getMaterials(Region region) {
    return () -> MaterialData.iterator(chunkSnapshots, region.getBlockVectorIterator());
  }
}
