package tc.oc.pgm.snapshot;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.block.BlockData;

/**
 * Utils to save, remove and paste blocks in some {@link Region} in some {@link Match} using the
 * {@link tc.oc.pgm.snapshot.SnapshotMatchModule} as memory.
 */
class BudgetWorldEdit {
  private static final BlockVector NO_OFFSET = new BlockVector(0, 0, 0);

  private final World world;
  private final WorldSnapshot snapshot;

  BudgetWorldEdit(World world, WorldSnapshot snapshot) {
    this.world = world;
    this.snapshot = snapshot;
  }

  /**
   * Places blocks in the region from the {@link SnapshotMatchModule} memory.
   *
   * @param region region where the blocks were when they got saved
   * @param offset the offset to add when placing blocks
   */
  public void placeBlocks(Region region, BlockVector offset, boolean update) {
    if (offset == null) offset = NO_OFFSET;
    for (BlockData blockData : snapshot.getMaterials(region)) {
      blockData.applyTo(blockData.getBlock(world, offset), update);
    }
  }

  /**
   * "Removes" all blocks in some {@link Region} by setting all blocks to {@link Material#AIR}.
   *
   * @param region The region to remove blocks from
   * @param offset an offset to add to the region coordinates if the blocks were offset when placed
   */
  public void removeBlocks(Region region, BlockVector offset, boolean update) {
    if (offset == null) offset = NO_OFFSET;
    for (BlockData blockData : snapshot.getMaterials(region)) {
      Block block = blockData.getBlock(world, offset);
      // Ignore if already air
      if (!block.getType().equals(Material.AIR)) block.setType(Material.AIR, update);
    }
  }
}
