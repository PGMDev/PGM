package tc.oc.pgm.snapshot;

import java.util.Iterator;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.MaterialDataWithLocation;

/**
 * Utils to save, remove and paste blocks in some {@link Region} in some {@link Match} using the
 * {@link tc.oc.pgm.snapshot.SnapshotMatchModule} as memory.
 */
public class BudgetWorldEdit {

  private final Match match;

  BudgetWorldEdit(Match match) {
    this.match = match;
  }

  /**
   * Saves blocks to the memory
   *
   * @param region the region to save
   * @param includeAir if air should be considered as a part of the structure
   * @param clearAfterSave whether to clear the blocks inside this region after saving them
   */
  public void saveBlocks(Region region, boolean includeAir, boolean clearAfterSave) {
    final World world = match.getWorld();
    for (BlockVector blockVector : region.getBlockVectors()) {
      Block block = world.getBlockAt(blockVector.toLocation(world));
      if (!includeAir && block.getType().equals(Material.AIR)) continue;
      SnapshotMatchModule smm = match.needModule(SnapshotMatchModule.class);
      smm.saveSnapshot(block);

      if (clearAfterSave) {
        block.setType(Material.AIR);
      }
    }
  }

  /**
   * "Removes" all blocks in some {@link Region} by setting all blocks to {@link Material#AIR}.
   *
   * @param region The region to remove blocks from
   * @param offset an offset to add to the region coordinates if the blocks were offset when placed
   */
  public void removeBlocks(Region region, Vector offset) {
    final World world = match.getWorld();
    for (BlockVector blockVector : region.getBlockVectors()) {
      Block block = world.getBlockAt(blockVector.toLocation(world).add(offset));
      if (!block.getType().equals(Material.AIR)) block.setType(Material.AIR);
    }
  }

  /**
   * Places blocks from the {@link tc.oc.pgm.snapshot.SnapshotMatchModule} memory.
   *
   * @param originalLocation region where the blocks were when they got saved
   * @param offset the offset to add when placing blocks
   * @param includeAir whether to place air if it's found in the memory
   * @see #saveBlocks(Region, boolean, boolean)
   */
  public void pasteBlocks(Region originalLocation, Vector offset, boolean includeAir) {
    final SnapshotMatchModule smm = match.needModule(SnapshotMatchModule.class);
    final World world = match.getWorld();
    final Iterator<MaterialDataWithLocation> blockStates =
        smm.getOriginalMaterialData(
            originalLocation,
            includeAir ? material -> true : material -> !material.equals(Material.AIR));
    while (blockStates.hasNext()) {
      MaterialDataWithLocation dataWithLocation = blockStates.next();
      BlockState state =
          world
              .getBlockAt(
                  dataWithLocation.vector.getBlockX() + offset.getBlockX(),
                  dataWithLocation.vector.getBlockY() + offset.getBlockY(),
                  dataWithLocation.vector.getBlockZ() + offset.getBlockZ())
              .getState();
      state.setMaterialData(dataWithLocation.data);
      state.update(true, true);
    }
  }
}
