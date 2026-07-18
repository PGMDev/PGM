package tc.oc.pgm.platform.modern.modules.behavior;

import javax.annotation.Nullable;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.looking.LookBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.PathingBehavior;

public class BehaviorDefinition extends SelfIdentifyingFeatureDefinition {
  private final boolean avoidDanger;
  private final @Nullable CombatBehavior combatBehavior;
  private final @Nullable LookBehavior lookBehavior;
  private final @Nullable PathingBehavior pathingBehavior;

  public BehaviorDefinition(
      @Nullable String id,
      boolean avoidDanger,
      @Nullable CombatBehavior combatBehavior,
      @Nullable LookBehavior lookBehavior,
      @Nullable PathingBehavior pathingBehavior) {
    super(id);
    this.avoidDanger = avoidDanger;
    this.combatBehavior = combatBehavior;
    this.lookBehavior = lookBehavior;
    this.pathingBehavior = pathingBehavior;
  }

  public boolean isAvoidDanger() {
    return avoidDanger;
  }

  public @Nullable CombatBehavior getCombatBehavior() {
    return combatBehavior;
  }

  public @Nullable LookBehavior getLookBehavior() {
    return lookBehavior;
  }

  public @Nullable PathingBehavior getPathingBehavior() {
    return pathingBehavior;
  }
}
