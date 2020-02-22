package tc.oc.pgm.tracker.trackers;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.tracker.damage.BlockInfo;
import tc.oc.pgm.tracker.damage.OwnerInfo;
import tc.oc.pgm.tracker.damage.PhysicalInfo;
import tc.oc.pgm.tracker.damage.TrackerInfo;
import tc.oc.util.ClassLogger;

/** Tracks the ownership of {@link Block}s and resolves damage caused by them */
public class BlockTracker implements Listener {

  private final Logger logger;
  private final Map<Block, TrackerInfo> blocks = new HashMap<>();
  private final Map<Block, Material> materials = new HashMap<>();

  public BlockTracker(Match match) {
    this.logger = ClassLogger.get(match.getLogger(), getClass());
  }

  public PhysicalInfo resolveBlock(Block block) {
    TrackerInfo info = blocks.get(block);
    if (info instanceof PhysicalInfo) {
      return (PhysicalInfo) info;
    } else if (info instanceof OwnerInfo) {
      return new BlockInfo(block.getState(), ((OwnerInfo) info).getOwner());
    } else {
      return new BlockInfo(block.getState());
    }
  }

  public @Nullable TrackerInfo resolveInfo(Block block) {
    return blocks.get(block);
  }

  public @Nullable <T extends TrackerInfo> T resolveInfo(Block block, Class<T> infoType) {
    TrackerInfo info = blocks.get(block);
    return infoType.isInstance(info) ? infoType.cast(info) : null;
  }

  public @Nullable ParticipantState getOwner(Block block) {
    OwnerInfo info = resolveInfo(block, OwnerInfo.class);
    return info == null ? null : info.getOwner();
  }

  public void trackBlockState(
      Block block, @Nullable Material material, @Nullable TrackerInfo info) {
    checkNotNull(block);
    if (info != null) {
      blocks.put(block, info);
      if (material != null) {
        materials.put(block, material);
      } else {
        materials.remove(block);
      }
      logger.fine("Track block=" + block + " world=" + material + " info=" + info);
    } else {
      clearBlock(block);
    }
  }

  public void trackBlockState(BlockState state, @Nullable TrackerInfo info) {
    checkNotNull(state);
    trackBlockState(state.getBlock(), state.getMaterial(), info);
  }

  public void clearBlock(Block block) {
    checkNotNull(block);
    blocks.remove(block);
    materials.remove(block);
    logger.fine("Clear block=" + block);
  }

  boolean isPlaced(BlockState state) {
    // If block was registered with a specific world, check that the new state
    // has the same world, otherwise assume the block is still placed.
    Material material = materials.get(state.getBlock());
    return material == null || material == state.getMaterial();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTransform(BlockTransformEvent event) {
    if (event.getCause() instanceof BlockPistonEvent) return;

    Block block = event.getOldState().getBlock();
    TrackerInfo info = blocks.get(block);
    if (info != null && !isPlaced(event.getNewState())) {
      clearBlock(block);
    }
  }

  private void handleMove(Collection<Block> blocks, BlockFace direction) {
    Map<Block, TrackerInfo> keepInfo = new HashMap<>();
    Map<Block, Material> keepMaterials = new HashMap<>();
    List<Block> remove = new ArrayList<>();

    for (Block block : blocks) {
      TrackerInfo info = this.blocks.get(block);
      if (info != null) {
        remove.add(block);
        keepInfo.put(block.getRelative(direction), info);

        Material material = materials.get(block);
        if (material != null) {
          keepMaterials.put(block, material);
        }
      }
    }

    for (Block block : remove) {
      TrackerInfo info = keepInfo.remove(block);
      if (info != null) {
        this.blocks.put(block, info);

        Material material = keepMaterials.get(block);
        if (material != null) {
          this.materials.put(block, material);
        }
      } else {
        this.blocks.remove(block);
        this.materials.remove(block);
      }
    }

    this.blocks.putAll(keepInfo);
    this.materials.putAll(keepMaterials);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPistonExtend(BlockPistonExtendEvent event) {
    handleMove(event.getBlocks(), event.getDirection());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPistonRetract(BlockPistonRetractEvent event) {
    handleMove(event.getBlocks(), event.getDirection());
  }
}
