package tc.oc.pgm.structure;

import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.snapshot.SnapshotMatchModule;

public class DynamicStructure implements Feature<DynamicStructureDefinition> {

  private final SnapshotMatchModule smm;
  private final StructureDefinition structure;
  private final BlockVector offset;
  private final DynamicStructureDefinition definition;

  // Since the passive filter can skip placing the structure,
  // we need to keep track of whether its placed or not if we
  // want to avoid unnecessary clears.
  private boolean placed;

  DynamicStructure(DynamicStructureDefinition definition, Match match) {
    this.smm = match.needModule(SnapshotMatchModule.class);
    this.definition = definition;
    this.structure = this.definition.getStructureDefinition();
    this.offset = this.definition.getOffset();

    // Position is the same as original structure, and it's not cleared
    this.placed = offset.equals(new Vector()) && !structure.clearSource();
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

    smm.placeBlocks(structure.getRegion(), offset, structure.includeAir());
  }

  /** Remove the structure from the world */
  public void clear() {
    if (!placed) return;
    placed = false;
    smm.removeBlocks(structure.getRegion(), offset, structure.includeAir());
  }
}
