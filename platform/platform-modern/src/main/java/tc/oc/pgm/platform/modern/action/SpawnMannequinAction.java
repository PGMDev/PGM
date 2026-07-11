package tc.oc.pgm.platform.modern.action;

import tc.oc.pgm.action.actions.AbstractAction;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinDefinition;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinMatchModule;
import tc.oc.pgm.util.math.Formula;

public class SpawnMannequinAction<B extends Filterable<?>> extends AbstractAction<B> {

  FeatureReference<MannequinDefinition> mannequinRef;
  private final Formula<B> xformula;
  private final Formula<B> yformula;
  private final Formula<B> zformula;

  public SpawnMannequinAction(
      Class<B> scope,
      FeatureReference<MannequinDefinition> mannequinRef,
      Formula<B> xformula,
      Formula<B> yformula,
      Formula<B> zformula) {
    super(scope);
    this.mannequinRef = mannequinRef;
    this.xformula = xformula;
    this.yformula = yformula;
    this.zformula = zformula;
  }

  @Override
  public void trigger(B b) {
    b.getMatch()
        .needModule(MannequinMatchModule.class)
        .spawn(mannequinRef.get(), xformula.apply(b), yformula.apply(b), zformula.apply(b));
  }
}
