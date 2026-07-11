package tc.oc.pgm.platform.modern.modules.mannequin;

import java.util.UUID;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.util.skin.Skin;

public class MannequinDefinition extends SelfIdentifyingFeatureDefinition {
  private final String name;
  private final UUID uuid;
  private final Skin skin;
  private final boolean silent;
  private final boolean invulnerable;
  private final TextColor glowing;
  private final float health;
  private final MannequinPose pose;
  private final boolean immovable;
  private final SkinPart.SkinLayers layers;

  public MannequinDefinition(
      @Nullable String id,
      String name,
      UUID uuid,
      Skin skin,
      boolean silent,
      boolean invulnerable,
      @Nullable TextColor glowing,
      float health,
      MannequinPose pose,
      boolean immovable,
      SkinPart.SkinLayers layers) {
    super(id);
    this.name = name;
    this.uuid = uuid;
    this.skin = skin;
    this.silent = silent;
    this.invulnerable = invulnerable;
    this.glowing = glowing;
    this.health = health;
    this.pose = pose;
    this.immovable = immovable;
    this.layers = layers;
  }

  public String getName() { return name; }

  public UUID getUuid() { return uuid; }

  public Skin getSkin() { return skin; }

  public boolean isSilent() { return silent; }

  public boolean isInvulnerable() { return invulnerable; }

  public TextColor getGlowing() { return glowing; }

  public float getHealth() { return health; }

  public MannequinPose getPose() { return pose; }

  public boolean isImmovable() { return immovable; }

  public SkinPart.SkinLayers getLayers() { return layers; }

}
