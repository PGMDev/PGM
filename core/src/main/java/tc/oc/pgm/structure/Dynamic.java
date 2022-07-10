package tc.oc.pgm.structure;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.BudgetWorldEdit;

public class Dynamic implements Feature<DynamicDefinition> {

  private final Match match;
  final StructureDefinition structureDefinition;
  final Vector offset;
  private final DynamicDefinition dynamicDefinition;

  // Since the passive filter can skip placing the structure,
  // we need to keep track of whether its placed or not if we
  // want to avoid unnecessary clears.
  boolean placed;

  Dynamic(DynamicDefinition dynamicDefinition, Match match) {
    this.match = match;
    this.dynamicDefinition = dynamicDefinition;
    this.structureDefinition = this.dynamicDefinition.getStructureDefinition();
    this.offset = this.findOffset();

    BudgetWorldEdit.saveBlocks(
        structureDefinition.getRegion(),
        structureDefinition.includeAir(),
        structureDefinition.clearSource(),
        match);

    this.placed = !structureDefinition.clearSource();
  }

  private Vector findOffset() {
    Vector position = this.dynamicDefinition.getPosition();
    Vector offset = this.dynamicDefinition.getOffset();
    if (position != null) {
      return position.subtract(this.structureDefinition.getOrigin());
    } else return offset != null ? offset : new Vector();
  }

  @Override
  public String getId() {
    return this.dynamicDefinition.getId();
  }

  @Override
  public DynamicDefinition getDefinition() {
    return this.dynamicDefinition;
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
