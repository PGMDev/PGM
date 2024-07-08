package tc.oc.pgm.action.actions;

import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.structure.StructureDefinition;
import tc.oc.pgm.util.math.Formula;

public class PasteStructureAction<T extends Filterable<?>> extends AbstractAction<T> {

  private final Formula<T> xformula;
  private final Formula<T> yformula;
  private final Formula<T> zformula;
  private final FeatureReference<StructureDefinition> structureReference;

  public PasteStructureAction(
      Class<T> scope,
      Formula<T> xformula,
      Formula<T> yformula,
      Formula<T> zformula,
      FeatureReference<StructureDefinition> structureReference) {
    super(scope);
    this.xformula = xformula;
    this.yformula = yformula;
    this.zformula = zformula;
    this.structureReference = structureReference;
  }

  @Override
  public void trigger(T t) {
    BlockVector placeLocation = new BlockVector(
        xformula.applyAsDouble(t), yformula.applyAsDouble(t), zformula.applyAsDouble(t));

    structureReference.get().getStructure(t.getMatch()).placeAbsolute(placeLocation);
  }
}
