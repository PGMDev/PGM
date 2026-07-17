package tc.oc.pgm.platform.modern.action.actions;

import java.util.Optional;
import java.util.UUID;
import tc.oc.pgm.action.actions.AbstractAction;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinDefinition;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinMatchModule;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.skin.Skin;

public class SpawnMannequinAction<B extends Filterable<?>> extends AbstractAction<B> {

  private final FeatureReference<MannequinDefinition> mannequinRef;
  private final Formula<B> xformula;
  private final Formula<B> yformula;
  private final Formula<B> zformula;
  private final Optional<Formula<B>> yawFormula;
  private final Optional<Formula<B>> pitchFormula;

  public SpawnMannequinAction(
      Class<B> scope,
      FeatureReference<MannequinDefinition> mannequinRef,
      Formula<B> xformula,
      Formula<B> yformula,
      Formula<B> zformula,
      Optional<Formula<B>> yawFormula,
      Optional<Formula<B>> pitchFormula) {
    super(scope);
    this.mannequinRef = mannequinRef;
    this.xformula = xformula;
    this.yformula = yformula;
    this.zformula = zformula;
    this.yawFormula = yawFormula;
    this.pitchFormula = pitchFormula;
  }

  @Override
  public void trigger(B b) {
    float yaw = yawFormula.map(f -> (float) f.apply(b)).orElse(0f);
    float pitch = pitchFormula.map(f -> (float) f.apply(b)).orElse(0f);
    MannequinDefinition definition = mannequinRef.get();
    UUID uuidOverride = null;
    Skin skinOverride = null;
    if (definition.isPlayerProfile()) {
      if (!(b instanceof MatchPlayer player)) return;
      ;
      uuidOverride = player.getId();
      skinOverride = PGM.get().getDatastore().getSkin(uuidOverride);
    }

    b.getMatch()
        .needModule(MannequinMatchModule.class)
        .spawn(
            mannequinRef.get(),
            xformula.apply(b),
            yformula.apply(b),
            zformula.apply(b),
            yaw,
            pitch,
            uuidOverride,
            skinOverride);
  }
}
