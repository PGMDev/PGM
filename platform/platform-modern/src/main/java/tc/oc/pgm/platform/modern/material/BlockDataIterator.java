package tc.oc.pgm.platform.modern.material;

import java.util.Iterator;
import java.util.Map;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.block.BlockData;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.material.MaterialData;

/**
 * Works in a similar fashion to {@link tc.oc.pgm.util.block.CuboidBlockIterator}. Implements both
 * {@link MaterialData} and {@link Iterator}, changes its own state while iterating, and returns
 * itself from {@link #next()}. In this way, it avoids creating any objects while iterating. It
 * additionally provides no methods to mutate the state.
 */
class BlockDataIterator implements Iterator<BlockData>, ModernBlockMaterialData, BlockData {

  private final Map<ChunkVector, ChunkSnapshot> chunks;
  private final Iterator<BlockVector> vectors;

  private ChunkVector chunkVector = null;
  private ChunkSnapshot snapshot = null;

  private BlockVector blockVector;
  private org.bukkit.block.data.BlockData data;

  BlockDataIterator(Map<ChunkVector, ChunkSnapshot> chunks, Iterator<BlockVector> vectors) {
    this.chunks = chunks;
    this.vectors = vectors;
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
      snapshot = chunks.get(chunkVector);
    }

    // Equivalent to chunkVector.worldToChunk(blockVector), but avoids allocations
    int offsetX = blockVector.getBlockX() - chunkVector.getBlockMinX();
    int offsetY = blockVector.getBlockY();
    int offsetZ = blockVector.getBlockZ() - chunkVector.getBlockMinZ();

    data = snapshot.getBlockData(offsetX, offsetY, offsetZ);

    return this;
  }

  @Override
  public Material getItemType() {
    return this.data.getMaterial();
  }

  @Override
  public org.bukkit.block.data.BlockData getBlock() {
    return data;
  }

  @Override
  public BlockVector getBlockVector() {
    return blockVector;
  }

  @Override
  public FallingBlock spawnFallingBlock(Location location) {
    return location
        .getWorld()
        .spawn(location, FallingBlock.class, fallingBlock -> fallingBlock.setBlockData(data));
  }
}
