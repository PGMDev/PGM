package tc.oc.pgm.structure;

import org.bukkit.Material;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.snapshot.WorldSnapshot;

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
    vector.subtract(getDefinition().getOrigin());
    place(vector, update);
  }
}
