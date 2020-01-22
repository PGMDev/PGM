package tc.oc.pgm.snapshot;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.chunk.ChunkVector;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.renewable.RenewableMatchModule;

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
    public Collection<Class<? extends MatchModule>> getSoftDependencies() {
      return ImmutableList.of(
          RenewableMatchModule.class); // Only needs to load if Renewables are loaded
    }

    @Override
    public SnapshotMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new SnapshotMatchModule(match);
    }
  }

  private final Match match;
  private final Map<ChunkVector, ChunkSnapshot> chunkSnapshots = new HashMap<>();

  private SnapshotMatchModule(Match match) {
    this.match = match;
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
      return match.getWorld().getBlockAt(x, y, z).getState().getMaterialData();
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

  // Listen on lowest priority so that the original block is available to other handlers of this
  // event
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onBlockChange(BlockTransformEvent event) {
    Chunk chunk = event.getOldState().getChunk();
    ChunkVector chunkVector = ChunkVector.of(chunk);
    if (!chunkSnapshots.containsKey(chunkVector)) {
      match.getLogger().fine("Copying chunk at " + chunkVector);
      ChunkSnapshot chunkSnapshot = chunk.getChunkSnapshot();
      chunkSnapshot.updateBlock(
          event
              .getOldState()); // ChunkSnapshot is very likely to have the post-event state already,
      // so we have to correct it
      chunkSnapshots.put(chunkVector, chunkSnapshot);
    }
  }
}
