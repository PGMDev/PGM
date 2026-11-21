package tc.oc.pgm.structure;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.snapshot.WorldSnapshot;
import tc.oc.pgm.util.block.BlockData;

public class Structure implements Feature<StructureDefinition> {

  private final StructureDefinition definition;
  private final WorldSnapshot snapshot;
  private final Region region;

  public Structure(StructureDefinition definition, Match match, WorldSnapshot snapshot) {
    this.definition = definition;
    this.snapshot = snapshot;

    if (definition.includeAir()) this.region = definition.getRegion();
    else
      this.region = FiniteBlockRegion.fromWorld(
          definition.getRegion(),
          match.getWorld(),
          b -> b.getType() != Material.AIR,
          match.getMap().getProto());

    if (region.getBounds().isEmpty()) {
      match
          .getLogger()
          .warning("No blocks found in structure "
              + definition.getId()
              + "; it will be empty"
              + (definition.includeAir()
                  ? ""
                  : " (set air=\"true\" to capture air, or check its region)"));
    }

    snapshot.saveRegion(region);
    if (definition.clearSource()) snapshot.removeBlocks(region, new BlockVector(), false);
  }

  @Override
  public String getId() {
    return getDefinition().getId();
  }

  @Override
  public StructureDefinition getDefinition() {
    return definition;
  }

  public Region getRegion() {
    return region;
  }

  public void place(BlockVector offset, boolean update) {
    snapshot.placeBlocks(region, offset, update);
  }

  public void placeAbsolute(BlockVector vector, boolean update) {
    vector.subtract(getRegion().getBounds().getBlockMin());
    place(vector, update);
  }

  /**
   * Find the first block of this structure that doesn't match the world, when placed with the given
   * offset.
   *
   * @param world the world to compare against
   * @param offset the offset the structure is placed at
   * @param firstCheck world position to check before the rest, usually the last known mismatch
   * @return the world position of the first mismatch, or null if the whole structure matches
   */
  public @Nullable BlockVector findMismatch(
      World world, BlockVector offset, BlockVector firstCheck) {
    int fx = firstCheck.getBlockX(), fy = firstCheck.getBlockY(), fz = firstCheck.getBlockZ();
    int sx = fx - offset.getBlockX(), sy = fy - offset.getBlockY(), sz = fz - offset.getBlockZ();

    if (region.getStatic().contains(new BlockVector(sx, sy, sz))
        && !snapshot.getOriginalMaterial(sx, sy, sz).sameMaterialAs(world.getBlockAt(fx, fy, fz))) {
      return firstCheck.clone();
    }

    for (BlockData blockData : snapshot.getMaterials(region)) {
      if (!blockData.sameMaterialAs(world, offset)) {
        BlockVector pos = blockData.getBlockVector();
        return new BlockVector(
            pos.getBlockX() + offset.getBlockX(),
            pos.getBlockY() + offset.getBlockY(),
            pos.getBlockZ() + offset.getBlockZ());
      }
    }
    return null;
  }
}
