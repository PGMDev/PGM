package tc.oc.pgm.snapshot;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.BlockData;

/**
 * Utils to save, remove and paste blocks in some {@link Region} in some {@link Match} using the
 * {@link tc.oc.pgm.snapshot.SnapshotMatchModule} as memory.
 */
class BudgetWorldEdit {

  private final Match match;
  private final SnapshotMatchModule smm;

  BudgetWorldEdit(Match match, SnapshotMatchModule snapshotMatchModule) {
    this.match = match;
    this.smm = snapshotMatchModule;
  }

  /**
   * "Removes" all blocks in some {@link Region} by setting all blocks to {@link Material#AIR}.
   *
   * @param region The region to remove blocks from
   * @param offset an offset to add to the region coordinates if the blocks were offset when placed
   * @param includeAir if blocks that originally were air should be included or not
   */
  public void removeBlocks(Region region, BlockVector offset, boolean includeAir) {
    final World world = match.getWorld();
    for (BlockData blockData : smm.getOriginalMaterialData(region)) {
      if (!includeAir && blockData.data.getItemType() == Material.AIR) continue;

      Block block = blockData.getBlock(world, offset);
      if (!block.getType().equals(Material.AIR)) block.setType(Material.AIR);
    }
  }

  /**
   * Places blocks from the {@link SnapshotMatchModule} memory.
   *
   * @param region region where the blocks were when they got saved
   * @param offset the offset to add when placing blocks
   * @param includeAir whether to place air if it's found in the memory
   */
  public void pasteBlocks(Region region, BlockVector offset, boolean includeAir) {
    final World world = match.getWorld();
    for (BlockData blockData : smm.getOriginalMaterialData(region)) {
      if (!includeAir && blockData.data.getItemType() == Material.AIR) continue;

      BlockState state = blockData.getBlock(world, offset).getState();
      state.setMaterialData(blockData.data);
      state.update(true, true);
    }
  }
}
