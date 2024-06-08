package tc.oc.pgm.platform.sportpaper;

import java.util.Iterator;
import java.util.Map;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
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
@SuppressWarnings("deprecation")
class BlockDataIterator implements Iterator<BlockData>, BlockData {

  private final Map<ChunkVector, ChunkSnapshot> chunks;
  private final Iterator<BlockVector> vectors;

  private ChunkVector chunkVector = null;
  private ChunkSnapshot snapshot = null;

  private BlockVector blockVector;
  private int materialId;
  private int data;

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

    // Calling getMaterialData would cause an allocation, so instead use raw types
    materialId = snapshot.getBlockTypeId(offsetX, offsetY, offsetZ);
    data = snapshot.getBlockData(offsetX, offsetY, offsetZ);

    return this;
  }

  @Override
  public Material getItemType() {
    return Material.getMaterial(this.materialId);
  }

  @Override
  public org.bukkit.material.MaterialData getBukkit() {
    return new org.bukkit.material.MaterialData(materialId, (byte) data);
  }

  @Override
  public void applyTo(Block block, boolean update) {
    block.setTypeIdAndData(materialId, (byte) data, update);
  }

  @Override
  public void applyTo(BlockState block) {
    block.setMaterialData(new org.bukkit.material.MaterialData(materialId, (byte) data));
  }

  @Override
  public void applyTo(ItemStack item) {
    item.setData(getBukkit());
  }

  @Override
  public BlockVector getBlockVector() {
    return blockVector;
  }

  @Override
  public int encoded() {
    throw new UnsupportedOperationException();
  }
}
