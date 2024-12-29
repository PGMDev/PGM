package tc.oc.pgm.snapshot;

import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;

/**
 * Keeps a snapshot of the block state of the entire match world at build time, using a
 * copy-on-write strategy. This module does nothing on its own, but other modules can use it to
 * query for the original world of the map.
 *
 * <p>The correct functioning of this module depends on EVERY block change firing a
 * {@link BlockTransformEvent}, without exception.
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
  // Represents the world state before any changes have been applied to it
  private final WorldSnapshot snapshot;

  private SnapshotMatchModule(Match match) {
    this.match = match;
    this.snapshot = new WorldSnapshot(match.getWorld());
  }

  public MaterialData getOriginalMaterial(int x, int y, int z) {
    return snapshot.getOriginalMaterial(x, y, z);
  }

  public BlockMaterialData getOriginalMaterial(Vector pos) {
    return snapshot.getOriginalMaterial(pos);
  }

  public BlockState getOriginalBlock(int x, int y, int z) {
    return snapshot.getOriginalBlock(x, y, z);
  }

  public BlockState getOriginalBlock(Vector pos) {
    return snapshot.getOriginalBlock(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
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
   * @param oldState optional block state to write on the snapshot
   */
  public void saveSnapshot(ChunkVector cv, @Nullable BlockState oldState) {
    snapshot.saveSnapshot(cv, oldState);
  }

  public WorldSnapshot getOriginalSnapshot() {
    return snapshot;
  }
}
