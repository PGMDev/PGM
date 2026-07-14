package tc.oc.pgm.platform.modern.modules.behavior;

import javax.annotation.Nullable;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatBehavior;

public class BehaviorDefinition extends SelfIdentifyingFeatureDefinition {
  private final @Nullable CombatBehavior combatBehavior;

  public BehaviorDefinition(@Nullable String id, @Nullable CombatBehavior combatBehavior) {
    super(id);
    this.combatBehavior = combatBehavior;
  }

  public @Nullable CombatBehavior getCombatBehavior() {
    return combatBehavior;
  }
}
