package tc.oc.pgm.util;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.snapshot.SnapshotMatchModule;

/**
 * Utils to save, remove and paste blocks in some {@link Region} in some {@link Match} using the
 * {@link SnapshotMatchModule} as memory.
 */
public class BudgetWorldEdit {

  private BudgetWorldEdit() {}

  /**
   * Saves blocks to the memory
   *
   * @param region the region to save
   * @param includeAir if air should be considered as a part of the structure
   * @param clearAfterSave whether to clear the blocks inside this region after saving them
   * @param match the match to save blocks in
   */
  public static void saveBlocks(
      Region region, boolean includeAir, boolean clearAfterSave, Match match) {
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
   * @param match the match to remove blocks from
   */
  public static void removeBlocks(Region region, Vector offset, Match match) {
    final World world = match.getWorld();
    for (BlockVector blockVector : region.getBlockVectors()) {
      Block block = world.getBlockAt(blockVector.toLocation(world).add(offset));
      if (!block.getType().equals(Material.AIR)) block.setType(Material.AIR);
    }
  }

  /**
   * Places blocks from the {@link SnapshotMatchModule} memory.
   *
   * @param originalLocation region where the blocks were when they got saved
   * @param offset the offset to add when placing blocks
   * @param includeAir whether to place air if it's found in the memory
   * @param match the match to place the blocks in
   * @see #saveBlocks(Region, boolean, boolean, Match)
   */
  public static void pasteBlocks(
      Region originalLocation, Vector offset, boolean includeAir, Match match) {
    final SnapshotMatchModule smm = match.needModule(SnapshotMatchModule.class);
    final World world = match.getWorld();
    final List<MaterialDataWithLocation> blockStates =
        smm.getOriginalMaterialData(
            originalLocation,
            includeAir ? material -> true : material -> !material.equals(Material.AIR));
    for (MaterialDataWithLocation dataWithLocation : blockStates) {
      BlockState state =
          world
              .getBlockAt(
                  dataWithLocation.x + offset.getBlockX(),
                  dataWithLocation.y + offset.getBlockY(),
                  dataWithLocation.z + offset.getBlockZ())
              .getState();
      state.setMaterialData(dataWithLocation.data);
      state.update(true, true);
    }
  }
}
