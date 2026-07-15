package tc.oc.pgm.platform.modern.modules.behavior;

import javax.annotation.Nullable;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.looking.LookBehavior;

public class BehaviorDefinition extends SelfIdentifyingFeatureDefinition {
  private final @Nullable CombatBehavior combatBehavior;
  private final @Nullable LookBehavior lookBehavior;

  public BehaviorDefinition(@Nullable String id, @Nullable CombatBehavior combatBehavior, @Nullable LookBehavior lookBehavior) {
    super(id);
    this.combatBehavior = combatBehavior;
    this.lookBehavior = lookBehavior;
  }

  public @Nullable CombatBehavior getCombatBehavior() {
    return combatBehavior;
  }

  public @Nullable LookBehavior getLookBehavior() {
    return lookBehavior;
  }
}
