package tc.oc.pgm.platform.modern.action.actions;

import java.util.Optional;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.action.actions.AbstractAction;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinDefinition;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinMatchModule;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinPose;
import tc.oc.pgm.util.math.Formula;

public class ModifyMannequinAction<B extends Filterable<?>> extends AbstractAction<B> {

  private final FeatureReference<MannequinDefinition> mannequinRef;
  private final @Nullable Component name;
  private final @Nullable Component description;
  private final @Nullable Boolean hideDescription;
  private final @Nullable Boolean hideTitles;
  private final @Nullable Float health;
  private final @Nullable MannequinPose pose;
  private final @Nullable Formula<B> xformula;
  private final @Nullable Formula<B> yformula;
  private final @Nullable Formula<B> zformula;
  private final Optional<Formula<B>> yawFormula;
  private final Optional<Formula<B>> pitchFormula;

  public ModifyMannequinAction(
      Class<B> scope,
      FeatureReference<MannequinDefinition> mannequinRef,
      @Nullable Component name,
      @Nullable Component description,
      @Nullable Boolean hideDescription,
      @Nullable Boolean hideTitles,
      @Nullable Float health,
      @Nullable MannequinPose pose,
      @Nullable Formula<B> xformula,
      @Nullable Formula<B> yformula,
      @Nullable Formula<B> zformula,
      Optional<Formula<B>> yawFormula,
      Optional<Formula<B>> pitchFormula) {
    super(scope);
    this.mannequinRef = mannequinRef;
    this.name = name;
    this.description = description;
    this.hideDescription = hideDescription;
    this.hideTitles = hideTitles;
    this.health = health;
    this.pose = pose;
    this.xformula = xformula;
    this.yformula = yformula;
    this.zformula = zformula;
    this.yawFormula = yawFormula;
    this.pitchFormula = pitchFormula;
  }

  @Override
  public void trigger(B b) {
    MannequinDefinition definition = mannequinRef.get();
    float yaw = yawFormula.map(f -> (float) f.apply(b)).orElse(0f);
    float pitch = pitchFormula.map(f -> (float) f.apply(b)).orElse(0f);
    b.getMatch().needModule(MannequinMatchModule.class).modify(definition.getId(), mannequin -> {
      if (name != null) mannequin.setName(name);
      if (description != null) mannequin.setDescription(description);
      if (hideDescription != null) mannequin.setHideDescription(hideDescription);
      if (hideTitles != null && hideTitles) mannequin.hideTitles();
      if (health != null) mannequin.setHealth(health);
      if (pose != null) mannequin.setPose(pose);

      if (xformula != null && yformula != null && zformula != null) {
        mannequin.teleport(
            xformula.apply(b),
            yformula.apply(b),
            zformula.apply(b),
            yawFormula.isPresent() ? yaw : mannequin.getYaw(),
            pitchFormula.isPresent() ? pitch : mannequin.getPitch());
      }
    });
  }
}
