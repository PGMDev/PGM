package tc.oc.pgm.action.actions;

import org.bukkit.util.BlockVector;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.structure.StructureDefinition;
import tc.oc.pgm.util.math.Formula;

public class PasteStructureAction<T extends Filterable<?>> extends AbstractAction<T> {

  private final Formula<T> xformula;
  private final Formula<T> yformula;
  private final Formula<T> zformula;
  private final FeatureReference<StructureDefinition> structureReference;
  private final boolean update;

  public PasteStructureAction(
      Class<T> scope,
      Formula<T> xformula,
      Formula<T> yformula,
      Formula<T> zformula,
      FeatureReference<StructureDefinition> structureReference,
      boolean update) {
    super(scope);
    this.xformula = xformula;
    this.yformula = yformula;
    this.zformula = zformula;
    this.structureReference = structureReference;
    this.update = update;
  }

  @Override
  public void trigger(T t, ActionContext context) {
    var locals = context.locals();
    var loc = new BlockVector(
        xformula.apply(t, locals), yformula.apply(t, locals), zformula.apply(t, locals));
    structureReference.get().getStructure(t.getMatch()).placeAbsolute(loc, update);
  }
}
