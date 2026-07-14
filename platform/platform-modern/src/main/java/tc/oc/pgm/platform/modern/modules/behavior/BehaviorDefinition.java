package tc.oc.pgm.platform.modern.modules.behavior;

import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatBehavior;

import javax.annotation.Nullable;

public class BehaviorDefinition extends SelfIdentifyingFeatureDefinition {
  private final @Nullable CombatBehavior combatBehavior;

  public BehaviorDefinition(
    @Nullable String id,
    @Nullable CombatBehavior combatBehavior) {
  super(id);
  this.combatBehavior = combatBehavior;
    }

    public @Nullable CombatBehavior getCombatBehavior() {
    return combatBehavior;
  }
}
