package tc.oc.pgm.platform.modern.action.actions;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.action.actions.AbstractAction;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.platform.modern.modules.behavior.BehaviorDefinition;
import tc.oc.pgm.platform.modern.modules.behavior.BehaviorMatchModule;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinDefinition;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinMatchModule;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinPose;
import tc.oc.pgm.platform.modern.modules.mannequin.SkinPart;
import tc.oc.pgm.platform.modern.modules.waypoint.WaypointDefinition;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.skin.Skin;

public class ModifyMannequinAction<B extends Filterable<?>> extends AbstractAction<B> {

  private final FeatureReference<MannequinDefinition> mannequinRef;
  private final @Nullable Component name;
  private final @Nullable Component description;
  private final @Nullable Boolean hideDescription;
  private final @Nullable Boolean hideTitles;
  private final @Nullable Float health;
  private final @Nullable Boolean silent;
  private final @Nullable Boolean invulnerable;
  private final @Nullable Boolean glowing;
  private final @Nullable Boolean immovable;
  private final @Nullable Boolean gravity;
  private final @Nullable Boolean physics;
  private final @Nullable Boolean onFire;
  private final boolean playerProfile;
  private final @Nullable UUID uuid;
  private final @Nullable Skin skin;
  private final @Nullable SkinPart.SkinLayers layers;
  private final @Nullable MannequinPose pose;
  private final @Nullable FeatureReference<WaypointDefinition> waypoint;
  private final @Nullable Boolean removeWaypoint;
  private final @Nullable FeatureReference<BehaviorDefinition> behavior;
  private final @Nullable Formula<B> xformula;
  private final @Nullable Formula<B> yformula;
  private final @Nullable Formula<B> zformula;
  private final Optional<Formula<B>> yawFormula;
  private final Optional<Formula<B>> pitchFormula;
  private final @Nullable Boolean despawn;

  public ModifyMannequinAction(
      Class<B> scope,
      FeatureReference<MannequinDefinition> mannequinRef,
      @Nullable Boolean despawn,
      @Nullable Component name,
      @Nullable Component description,
      @Nullable Boolean hideDescription,
      @Nullable Boolean hideTitles,
      @Nullable Float health,
      @Nullable Boolean silent,
      @Nullable Boolean invulnerable,
      @Nullable Boolean glowing,
      @Nullable Boolean immovable,
      @Nullable Boolean gravity,
      @Nullable Boolean physics,
      @Nullable Boolean onFire,
      boolean playerProfile,
      @Nullable UUID uuid,
      @Nullable Skin skin,
      @Nullable SkinPart.SkinLayers layers,
      @Nullable MannequinPose pose,
      @Nullable FeatureReference<WaypointDefinition> waypoint,
      @Nullable Boolean removeWaypoint,
      @Nullable FeatureReference<BehaviorDefinition> behavior,
      @Nullable Formula<B> xformula,
      @Nullable Formula<B> yformula,
      @Nullable Formula<B> zformula,
      Optional<Formula<B>> yawFormula,
      Optional<Formula<B>> pitchFormula) {
    super(scope);
    this.mannequinRef = mannequinRef;
    this.despawn = despawn;
    this.name = name;
    this.description = description;
    this.hideDescription = hideDescription;
    this.hideTitles = hideTitles;
    this.health = health;
    this.silent = silent;
    this.invulnerable = invulnerable;
    this.glowing = glowing;
    this.immovable = immovable;
    this.gravity = gravity;
    this.physics = physics;
    this.onFire = onFire;
    this.playerProfile = playerProfile;
    this.uuid = uuid;
    this.skin = skin;
    this.layers = layers;
    this.pose = pose;
    this.waypoint = waypoint;
    this.removeWaypoint = removeWaypoint;
    this.behavior = behavior;
    this.xformula = xformula;
    this.yformula = yformula;
    this.zformula = zformula;
    this.yawFormula = yawFormula;
    this.pitchFormula = pitchFormula;
  }

  @Override
  public void trigger(B b) {
    MannequinDefinition definition = mannequinRef.get();
    var mmm = b.getMatch().needModule(MannequinMatchModule.class);

    if (Boolean.TRUE.equals(despawn)) {
      mmm.despawn(definition.getId());
      return;
    }

    float yaw = yawFormula.map(f -> (float) f.apply(b)).orElse(0f);
    float pitch = pitchFormula.map(f -> (float) f.apply(b)).orElse(0f);

    final UUID resolvedUuid;
    final Skin resolvedSkin;
    if (playerProfile) {
      if (!(b instanceof MatchPlayer player)) return;
      resolvedUuid = player.getId();
      if (resolvedUuid == null) {
        return;
      }
      resolvedSkin = PGM.get().getDatastore().getSkin(resolvedUuid);
      if (resolvedSkin == null) {
        return;
      }
    } else {
      resolvedUuid = this.uuid;
      resolvedSkin = this.skin;
    }

    mmm.modify(definition.getId(), mannequin -> {
      if (name != null) mannequin.setName(name);
      if (description != null) mannequin.setDescription(description);
      if (hideDescription != null) mannequin.setHideDescription();
      if (hideTitles != null && hideTitles) mannequin.hideTitles();
      if (health != null) mannequin.setHealth(health);
      if (silent != null) mannequin.setSilent(silent);
      if (invulnerable != null) mannequin.setInvulnerable(invulnerable);
      if (glowing != null) mannequin.setGlowing(glowing);
      if (immovable != null) mannequin.setImmovable(immovable);
      if (gravity != null) mannequin.setGravity(gravity);
      if (physics != null) mannequin.setPhysics(physics);
      if (onFire != null) mannequin.setOnFire(onFire);

      if (resolvedUuid != null && resolvedSkin != null) {
        mannequin.setSkin(resolvedUuid, resolvedSkin);
        mannequin.setSkinLayers(SkinPart.SkinLayers.allOf());
      }
      if (layers != null) mannequin.setSkinLayers(layers);
      if (pose != null) mannequin.setPose(pose);

      if (waypoint != null) mmm.setWaypoint(mannequin, waypoint.get());
      if (removeWaypoint != null && removeWaypoint) mmm.removeWaypoint(mannequin);

      if (behavior != null) {
        b.getMatch()
            .needModule(BehaviorMatchModule.class)
            .modifyBehavior(mannequin, behavior.get());
      }

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
