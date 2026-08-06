package tc.oc.pgm.structure;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.snapshot.WorldSnapshot;
import tc.oc.pgm.util.block.BlockData;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;

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

  public boolean findMismatch(World world, BlockVector offset, BlockVector out) {
    BlockMaterialData snapshotMaterial = snapshot.getOriginalMaterial(
        out.getBlockX() - offset.getBlockX(),
        out.getBlockY() - offset.getBlockY(),
        out.getBlockZ() - offset.getBlockZ());
    if (snapshotMaterial.encoded()
        != MaterialData.block(world.getBlockAt(out.getBlockX(), out.getBlockY(), out.getBlockZ()))
            .encoded()) {
      return true;
    }

    for (BlockData blockData : snapshot.getMaterials(region)) {
      if (!blockData.sameMaterialAs(world, offset)) {
        BlockVector sv = blockData.getBlockVector();
        out.setX(sv.getBlockX() + offset.getBlockX());
        out.setY(sv.getBlockY() + offset.getBlockY());
        out.setZ(sv.getBlockZ() + offset.getBlockZ());
        return true;
      }
    }
    return false;
  }
}
