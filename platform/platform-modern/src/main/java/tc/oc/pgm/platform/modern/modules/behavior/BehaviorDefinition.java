package tc.oc.pgm.platform.modern.modules.behavior;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.evade.EvadeBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.home.HomeBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.looking.LookBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.PathingBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.tempt.TemptBehavior;

public class BehaviorDefinition extends SelfIdentifyingFeatureDefinition {
  private final boolean avoidDanger;
  private final boolean openDoors;
  private final boolean enterVehicles;
  private final @Nullable CombatBehavior combatBehavior;
  private final @Nullable HomeBehavior homeBehavior;
  private final @Nullable EvadeBehavior evadeBehavior;
  private final @Nullable TemptBehavior temptBehavior;
  private final @Nullable LookBehavior lookBehavior;
  private final @Nullable PathingBehavior pathingBehavior;

  public BehaviorDefinition(
      @Nullable String id,
      boolean avoidDanger,
      boolean openDoors,
      boolean enterVehicles,
      @Nullable CombatBehavior combatBehavior,
      @Nullable HomeBehavior homeBehavior,
      @Nullable EvadeBehavior evadeBehavior,
      @Nullable TemptBehavior temptBehavior,
      @Nullable LookBehavior lookBehavior,
      @Nullable PathingBehavior pathingBehavior) {
    super(id);
    this.avoidDanger = avoidDanger;
    this.openDoors = openDoors;
    this.enterVehicles = enterVehicles;
    this.combatBehavior = combatBehavior;
    this.homeBehavior = homeBehavior;
    this.evadeBehavior = evadeBehavior;
    this.temptBehavior = temptBehavior;
    this.lookBehavior = lookBehavior;
    this.pathingBehavior = pathingBehavior;
  }

  public boolean isAvoidDanger() {
    return avoidDanger;
  }

  public boolean isOpenDoors() {
    return openDoors;
  }

  public boolean isEnterVehicles() {
    return enterVehicles;
  }

  public @Nullable CombatBehavior getCombatBehavior() {
    return combatBehavior;
  }

  public @Nullable HomeBehavior getHomeBehavior() {
    return homeBehavior;
  }

  public @Nullable EvadeBehavior getEvadeBehavior() {
    return evadeBehavior;
  }

  public @Nullable TemptBehavior getTemptBehavior() {
    return temptBehavior;
  }

  public @Nullable LookBehavior getLookBehavior() {
    return lookBehavior;
  }

  public @Nullable PathingBehavior getPathingBehavior() {
    return pathingBehavior;
  }
}
