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

  // Ugly optimization, match material via primitive id
  private static final int AIR_ID = Material.AIR.getId();

  private final Match match;
  private final SnapshotMatchModule smm;

  BudgetWorldEdit(Match match, SnapshotMatchModule snapshotMatchModule) {
    this.match = match;
    this.smm = snapshotMatchModule;
  }

  /**
   * Places blocks in the region from the {@link SnapshotMatchModule} memory.
   *
   * @param region region where the blocks were when they got saved
   * @param offset the offset to add when placing blocks
   * @param includeAir whether to place air if it's found in the memory
   */
  public void placeBlocks(Region region, BlockVector offset, boolean includeAir) {
    final World world = match.getWorld();
    for (BlockData blockData : smm.getOriginalMaterials(region)) {
      if (!includeAir && blockData.getTypeId() == AIR_ID) continue;

      BlockState state = blockData.getBlock(world, offset).getState();
      state.setMaterialData(blockData.getMaterialData());
      state.update(true, true);
    }
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

    for (BlockData blockData : smm.getOriginalMaterials(region)) {
      if (!includeAir && blockData.getTypeId() == AIR_ID) continue;

      Block block = blockData.getBlock(world, offset);
      // Ignore if already air
      if (!block.getType().equals(Material.AIR)) block.setType(Material.AIR);
    }
  }
}
