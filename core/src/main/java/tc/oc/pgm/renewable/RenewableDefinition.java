package tc.oc.pgm.renewable;

import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.query.BlockQuery;

public class RenewableDefinition {
  public Region region;
  public Filter renewableBlocks;
  public Filter replaceableBlocks;
  public Filter shuffleableBlocks;
  public float renewalsPerSecond; // Blocks per second
  public boolean rateScaled; // Renewal rate is per-volume
  public boolean growAdjacent;
  public boolean particles;
  public boolean sound;
  public double avoidPlayersRange;

  public boolean isShuffleable(BlockState block) {
    return region.contains(block) && shuffleableBlocks.query(new BlockQuery(block)).isAllowed();
  }

  public boolean isReplaceable(BlockState block) {
    return region.contains(block) && replaceableBlocks.query(new BlockQuery(block)).isAllowed();
  }

  public boolean canRenew(BlockState original, BlockState current) {
    // Original block must be in the region and match the renewable filter
    if (!region.contains(original)) return false;
    BlockQuery originalQuery = new BlockQuery(original);
    if (!renewableBlocks.query(originalQuery).isAllowed()) return false;

    MaterialData originalMaterial = original.getMaterialData();
    MaterialData currentMaterial = current.getMaterialData();

    // If current world matches the original, block is already renewed
    if (originalMaterial.equals(currentMaterial)) return false;

    // If the original and current blocks are both shuffleable, block is already renewed
    if (shuffleableBlocks.query(originalQuery).isAllowed()
        && shuffleableBlocks.query(new BlockQuery(current)).isAllowed()) {
      return false;
    }

    // Otherwise, block is eligible to be renewed
    return true;
  }
}
