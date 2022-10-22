package tc.oc.pgm.structure;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.snapshot.BudgetWorldEdit;

public class DynamicStructure implements Feature<DynamicStructureDefinition> {

  private final Match match;
  private final StructureDefinition structureDefinition;
  private final Vector offset;
  private final DynamicStructureDefinition dynamicStructureDefinition;

  // Since the passive filter can skip placing the structure,
  // we need to keep track of whether its placed or not if we
  // want to avoid unnecessary clears.
  private boolean placed;

  DynamicStructure(DynamicStructureDefinition dynamicStructureDefinition, Match match) {
    this.match = match;
    this.dynamicStructureDefinition = dynamicStructureDefinition;
    this.structureDefinition = this.dynamicStructureDefinition.getStructureDefinition();
    this.offset = this.findOffset();

    BudgetWorldEdit.saveBlocks(
        structureDefinition.getRegion(),
        structureDefinition.includeAir(),
        structureDefinition.clearSource(),
        match);

    this.placed = !structureDefinition.clearSource();
  }

  private Vector findOffset() {
    Vector position = this.dynamicStructureDefinition.getPosition();
    Vector offset = this.dynamicStructureDefinition.getOffset();
    if (position != null) {
      return position.subtract(this.structureDefinition.getOrigin());
    } else return offset != null ? offset : new Vector();
  }

  @Override
  public String getId() {
    return this.dynamicStructureDefinition.getId();
  }

  @Override
  public DynamicStructureDefinition getDefinition() {
    return this.dynamicStructureDefinition;
  }

  /** Place the structure in the world */
  public void place() {
    if (!placed) {
      placed = true;
      BudgetWorldEdit.pasteBlocks(
          structureDefinition.getRegion(), offset, structureDefinition.includeAir(), match);
    }
  }

  /** Remove the structure from the world */
  public void clear() {
    if (placed) {
      placed = false;
      BudgetWorldEdit.removeBlocks(structureDefinition.getRegion(), offset, match);
    }
  }
}
