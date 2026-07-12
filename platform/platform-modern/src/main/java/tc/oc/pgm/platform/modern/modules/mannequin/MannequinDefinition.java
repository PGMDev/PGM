package tc.oc.pgm.platform.modern.modules.mannequin;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.MainHand;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.projectile.ClickAction;
import tc.oc.pgm.util.skin.Skin;

public class MannequinDefinition extends SelfIdentifyingFeatureDefinition {
  private final Component name;
  private final Component description;
  private final UUID uuid;
  private final Skin skin;
  private final boolean silent;
  private final boolean invulnerable;
  private final TextColor glowing;
  private final float health;
  private final MannequinPose pose;
  private final boolean immovable;
  private final MainHand mainHand;
  private final boolean gravity;
  private final SkinPart.SkinLayers layers;
  private final @Nullable Action<? super MatchPlayer> action;
  private final @Nullable ClickAction cause;

  public MannequinDefinition(
      @Nullable String id,
      Component name,
      Component description,
      UUID uuid,
      Skin skin,
      boolean silent,
      boolean invulnerable,
      @Nullable TextColor glowing,
      float health,
      MannequinPose pose,
      boolean immovable,
      MainHand mainHand,
      boolean gravity,
      SkinPart.SkinLayers layers,
      @Nullable Action<? super MatchPlayer> action,
      @Nullable ClickAction cause) {
    super(id);
    this.name = name;
    this.description = description;
    this.uuid = uuid;
    this.skin = skin;
    this.silent = silent;
    this.invulnerable = invulnerable;
    this.glowing = glowing;
    this.health = health;
    this.pose = pose;
    this.immovable = immovable;
    this.mainHand = mainHand;
    this.gravity = gravity;
    this.layers = layers;
    this.action = action;
    this.cause = cause;
  }

  public Component getName() {
    return name;
  }

  public Component getDescription() {
    return description;
  }

  public UUID getUuid() {
    return uuid;
  }

  public Skin getSkin() {
    return skin;
  }

  public boolean isSilent() {
    return silent;
  }

  public boolean isInvulnerable() {
    return invulnerable;
  }

  public TextColor getGlowing() {
    return glowing;
  }

  public float getHealth() {
    return health;
  }

  public MannequinPose getPose() {
    return pose;
  }

  public boolean isImmovable() {
    return immovable;
  }

  public MainHand getMainHand() {
    return mainHand;
  }

  public boolean hasGravity() {
    return gravity;
  }

  public SkinPart.SkinLayers getLayers() {
    return layers;
  }

  public @Nullable Action<? super MatchPlayer> getAction() {
    return action;
  }

  public @Nullable ClickAction getCause() {
    return cause;
  }
}
