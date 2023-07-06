package tc.oc.pgm.snapshot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.BlockData;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.MaterialDataProvider;
import tc.oc.pgm.util.nms.material.legacy.MaterialDataLegacy;

public class WorldSnapshot {

  private final World world;
  private final Map<ChunkVector, ChunkSnapshot> chunkSnapshots = new HashMap<>();
  private final BudgetWorldEdit worldEdit;

  public WorldSnapshot(World world) {
    this.world = world;
    this.worldEdit = new BudgetWorldEdit(world, this);
  }

  public MaterialData getOriginalMaterial(int x, int y, int z) {
    if (y < 0 || y >= 256) return MaterialDataProvider.from(Material.AIR);

    ChunkVector chunkVector = ChunkVector.ofBlock(x, y, z);
    ChunkSnapshot chunkSnapshot = chunkSnapshots.get(chunkVector);
    if (chunkSnapshot != null) {
      BlockVector chunkPos = chunkVector.worldToChunk(x, y, z);
      int x1 = chunkPos.getBlockX();
      int y1 = chunkPos.getBlockY();
      int z1 = chunkPos.getBlockZ();
      return MaterialDataProvider.from(chunkSnapshot, x1, y1, z1);
    } else {
      Block block = world.getBlockAt(x, y, z);
      return MaterialDataProvider.from(block);
    }
  }

  public BlockState getOriginalBlock(int x, int y, int z) {
    BlockState state = world.getBlockAt(x, y, z).getState();
    if (y < 0 || y >= 256) return state;

    ChunkVector chunkVector = ChunkVector.ofBlock(x, y, z);
    ChunkSnapshot chunkSnapshot = chunkSnapshots.get(chunkVector);
    if (chunkSnapshot != null) {
      BlockVector chunkPos = chunkVector.worldToChunk(x, y, z);

      int x1 = chunkPos.getBlockX();
      int y1 = chunkPos.getBlockY();
      int z1 = chunkPos.getBlockZ();
      MaterialDataProvider.from(chunkSnapshot, x1, y1, z1).apply(state);
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
    return () -> new BlockDataIterator(region);
  }

  /**
   * Works in a similar fashion to {@link tc.oc.pgm.util.block.CuboidBlockIterator}. Implements both
   * {@link BlockData} and {@link Iterator}, changes its own state while iterating, and returns
   * itself from {@link #next()}. In this way, it avoids creating any objects while iterating. It
   * additionally provides no methods to mutate the state.
   */
  private class BlockDataIterator implements Iterator<BlockData>, BlockData {

    private final Iterator<BlockVector> vectors;

    private ChunkVector chunkVector = null;
    private ChunkSnapshot snapshot = null;

    private BlockVector blockVector;
    private int encodedMaterial;

    private BlockDataIterator(Region region) {
      this.vectors = region.getBlockVectorIterator();
    }

    @Override
    public boolean hasNext() {
      return this.vectors.hasNext();
    }

    @Override
    public BlockData next() {
      blockVector = this.vectors.next();

      // If this block is in the same chunk as the previous one, keep using the same snapshot
      // without fetching a new one
      if (snapshot == null
          || blockVector.getBlockZ() >> 4 != chunkVector.getChunkZ()
          || blockVector.getBlockX() >> 4 != chunkVector.getChunkX()) {
        chunkVector = ChunkVector.ofBlock(blockVector);
        snapshot = chunkSnapshots.get(chunkVector);
      }

      // Equivalent to chunkVector.worldToChunk(blockVector), but avoids allocations
      int offsetX = blockVector.getBlockX() - chunkVector.getBlockMinX();
      int offsetY = blockVector.getBlockY();
      int offsetZ = blockVector.getBlockZ() - chunkVector.getBlockMinZ();

      // Calling getMaterialData would cause an allocation, so instead use raw types
      encodedMaterial = MaterialDataLegacy.encodedMaterialAt(snapshot, offsetX, offsetY, offsetZ);

      return this;
    }

    @Override
    public MaterialData getMaterialData() {
      return MaterialDataProvider.from(encodedMaterial);
    }

    @Override
    public BlockVector getBlockVector() {
      return blockVector;
    }
  }
}
