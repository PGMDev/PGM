package tc.oc.pgm.snapshot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.BlockData;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.nms.NMSHacks;

/**
 * Keeps a snapshot of the block state of the entire match world at build time, using a
 * copy-on-write strategy. This module does nothing on its own, but other modules can use it to
 * query for the original world of the map.
 *
 * <p>The correct functioning of this module depends on EVERY block change firing a {@link
 * BlockTransformEvent}, without exception.
 */
@ListenerScope(MatchScope.LOADED)
public class SnapshotMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<SnapshotMatchModule> {

    @Override
    public SnapshotMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new SnapshotMatchModule(match);
    }
  }

  private final Match match;
  private final Map<ChunkVector, ChunkSnapshot> chunkSnapshots = new HashMap<>();
  private final BudgetWorldEdit worldEdit;

  private SnapshotMatchModule(Match match) {
    this.match = match;
    this.worldEdit = new BudgetWorldEdit(match, this);
  }

  public MaterialData getOriginalMaterial(int x, int y, int z) {
    if (y < 0 || y >= 256) return new MaterialData(Material.AIR);

    ChunkVector chunkVector = ChunkVector.ofBlock(x, y, z);
    ChunkSnapshot chunkSnapshot = chunkSnapshots.get(chunkVector);
    if (chunkSnapshot != null) {
      BlockVector chunkPos = chunkVector.worldToChunk(x, y, z);
      return new MaterialData(
          chunkSnapshot.getBlockTypeId(
              chunkPos.getBlockX(), chunkPos.getBlockY(), chunkPos.getBlockZ()),
          (byte)
              chunkSnapshot.getBlockData(
                  chunkPos.getBlockX(), chunkPos.getBlockY(), chunkPos.getBlockZ()));
    } else {
      return match.getWorld().getBlockAt(x, y, z).getState().getData();
    }
  }

  public MaterialData getOriginalMaterial(Vector pos) {
    return getOriginalMaterial(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
  }

  public BlockState getOriginalBlock(int x, int y, int z) {
    BlockState state = match.getWorld().getBlockAt(x, y, z).getState();
    if (y < 0 || y >= 256) return state;

    ChunkVector chunkVector = ChunkVector.ofBlock(x, y, z);
    ChunkSnapshot chunkSnapshot = chunkSnapshots.get(chunkVector);
    if (chunkSnapshot != null) {
      BlockVector chunkPos = chunkVector.worldToChunk(x, y, z);
      state.setMaterialData(
          new MaterialData(
              chunkSnapshot.getBlockTypeId(
                  chunkPos.getBlockX(), chunkPos.getBlockY(), chunkPos.getBlockZ()),
              (byte)
                  chunkSnapshot.getBlockData(
                      chunkPos.getBlockX(), chunkPos.getBlockY(), chunkPos.getBlockZ())));
    }
    return state;
  }

  public BlockState getOriginalBlock(Vector pos) {
    return getOriginalBlock(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
  }

  /**
   * Get the original material data for a {@code region}.
   *
   * @param region the region to get block states from
   */
  public Iterable<BlockData> getOriginalMaterials(Region region) {
    return () -> new MaterialDataWithLocationIterator(region);
  }

  // Listen on lowest priority so that the original block is available to other handlers of this
  // event
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onBlockChange(BlockTransformEvent event) {
    if (event.getWorld() != match.getWorld()) return;
    saveSnapshot(ChunkVector.ofBlock(event.getBlock()), event.getOldState());
  }

  /**
   * Manually save the initial state of a block to the snapshot.
   *
   * @param cv the chunk vector to save
   * @param state optional block state to write on the snapshot
   */
  public void saveSnapshot(ChunkVector cv, @Nullable BlockState state) {
    if (!chunkSnapshots.containsKey(cv)) {
      this.match.getLogger().fine("Copying chunk at " + cv);

      ChunkSnapshot snapshot = cv.getChunk(match.getWorld()).getChunkSnapshot();

      // ChunkSnapshot is very likely to have the post-event state already,
      // so we have to correct it
      if (state != null) NMSHacks.updateChunkSnapshot(snapshot, state);

      chunkSnapshots.put(cv, snapshot);
    }
  }

  public void saveRegion(Region region) {
    region.getChunkPositions().forEach(cv -> this.saveSnapshot(cv, null));
  }

  public void placeBlocks(Region region, BlockVector offset, boolean includeAir) {
    worldEdit.placeBlocks(region, offset, includeAir);
  }

  public void removeBlocks(Region region, BlockVector offset, boolean includeAir) {
    worldEdit.removeBlocks(region, offset, includeAir);
  }

  /**
   * Works in a similar fashion to {@link tc.oc.pgm.util.block.CuboidBlockIterator}. Implements both
   * {@link BlockData} and {@link Iterator}, changes its own state while iterating, and returns
   * itself from {@link #next()}. In this way, it avoids creating any objects while iterating. It
   * additionally provides no methods to mutate the state.
   */
  private class MaterialDataWithLocationIterator implements Iterator<BlockData>, BlockData {

    private final Iterator<BlockVector> vectors;

    private ChunkVector chunkVector = null;
    private ChunkSnapshot snapshot = null;

    private BlockVector blockVector;
    private int materialId;
    private int data;

    private MaterialDataWithLocationIterator(Region region) {
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
      materialId = snapshot.getBlockTypeId(offsetX, offsetY, offsetZ);
      data = snapshot.getBlockData(offsetX, offsetY, offsetZ);

      return this;
    }

    @Override
    public int getTypeId() {
      return materialId;
    }

    @Override
    public int getData() {
      return data;
    }

    @Override
    public BlockVector getBlockVector() {
      return blockVector;
    }
  }
}
