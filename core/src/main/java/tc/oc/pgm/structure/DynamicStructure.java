package tc.oc.pgm.structure;

import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.regions.TranslatedRegion;
import tc.oc.pgm.snapshot.WorldSnapshot;

public class DynamicStructure implements Feature<DynamicStructureDefinition> {

  private final Structure structure;
  private final BlockVector offset;
  private final WorldSnapshot snapshot;
  private final DynamicStructureDefinition definition;
  private final Region region;

  // Since the passive filter can skip placing the structure,
  // we need to keep track of whether its placed or not if we
  // want to avoid unnecessary clears.
  private boolean placed;

  DynamicStructure(
      DynamicStructureDefinition definition, Structure structure, WorldSnapshot snapshot) {
    this.structure = structure;
    this.definition = definition;
    this.offset = this.definition.getOffset();
    this.snapshot = snapshot;
    this.region = new TranslatedRegion(structure.getRegion(), offset);

    // Save the blocks at original position before dynamic is placed
    snapshot.saveRegion(region);
  }

  @Override
  public String getId() {
    return this.definition.getId();
  }

  @Override
  public DynamicStructureDefinition getDefinition() {
    return this.definition;
  }

  /** Place the structure in the world */
  public void place() {
    if (placed) return;
    placed = true;
    structure.place(offset, definition.shouldUpdate());
  }

  /** Remove the structure from the world */
  public void clear() {
    if (!placed) return;
    placed = false;

    snapshot.placeBlocks(region, new BlockVector(), definition.shouldUpdate());
  }
}
