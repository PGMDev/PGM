package tc.oc.pgm.platform.modern.listeners;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.listeners.BlockPhysicsListener;

@NullMarked
public class ModernBlockPhysicsListener extends BlockPhysicsListener {
  private final Set<LoadingChunk> loadingChunks = new HashSet<>();

  @EventHandler(priority = EventPriority.LOWEST)
  public void onFluidLevelChange(FluidLevelChangeEvent event) {
    if (!allowPhysics(event.getBlock().getWorld())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onChunkLoad(ChunkLoadEvent event) {
    if (Match.isMatchWorld(event.getWorld())) {
      loadingChunks.add(LoadingChunk.of(event.getChunk()));
    }
  }

  @EventHandler
  public void onTickEnd(ServerTickEndEvent event) {
    loadingChunks.removeIf(chunk -> !chunk.isLoading());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldUnload(WorldUnloadEvent event) {
    UUID worldId = event.getWorld().getUID();
    loadingChunks.removeIf(chunk -> chunk.worldId().equals(worldId));
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onBlockDestroy(BlockDestroyEvent event) {
    Block block = event.getBlock();
    boolean loading =
        !loadingChunks.isEmpty() && loadingChunks.contains(LoadingChunk.of(block.getChunk()));

    if (!loading && allowPhysics(block.getWorld())) return;

    event.setCancelled(true);
    updateShapeWithoutDestroying(block);
  }

  /**
   * Paper calls BlockDestroyEvent for indirect block breakage. We want to suppress this as that can
   * include "block physics", but legacy maps have certain blocks in unsupported states that do
   * break when upgraded. This includes, notably, redstone lines to indicate void regions.
   * Cancelling BlockDestroyEvent maintains them, but block connections are not maintained in the
   * world upgrade. This method is called after cancelling the event to ensure block connections are
   * updated without the block actually breaking. We do the same for chunks that are not fully
   * loaded.
   */
  private void updateShapeWithoutDestroying(Block block) {
    ServerLevel level = ((CraftWorld) block.getWorld()).getHandle();
    BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
    BlockState state = level.getBlockState(pos);
    BlockState updated = state;

    for (Direction direction : Direction.values()) {
      BlockPos neighborPos = pos.relative(direction);
      BlockState candidate = updated.updateShape(
          level,
          level,
          pos,
          direction,
          neighborPos,
          level.getBlockState(neighborPos),
          level.getRandom());
      if (!candidate.isAir()) {
        updated = candidate;
      }
    }

    if (updated != state) {
      block.setBlockData(CraftBlockData.createData(updated), false);
    }
  }

  private record LoadingChunk(UUID worldId, int x, int z) {
    private static LoadingChunk of(Chunk chunk) {
      return new LoadingChunk(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    private boolean isLoading() {
      World world = Bukkit.getWorld(worldId);
      return world != null
          && world.isChunkLoaded(x, z)
          && world.getChunkAt(x, z).getLoadLevel() != Chunk.LoadLevel.ENTITY_TICKING;
    }
  }
}
