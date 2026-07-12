package tc.oc.pgm.platform.modern.modules.mannequin;

import com.destroystokyo.paper.SkinParts;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.metadata.FixedMetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.skin.Skin;

public class Mannequin {

  public static final String METADATA_KEY = "PGM_mannequin_";
  private final org.bukkit.entity.Mannequin entity;
  private final MannequinDefinition definition;

  public Mannequin(org.bukkit.entity.Mannequin entity, MannequinDefinition definition) {
    this.entity = entity;
    this.definition = definition;
  }

  public static Mannequin spawn(Location origin, MannequinDefinition definition) {
    org.bukkit.entity.Mannequin entity = origin
        .getWorld()
        .spawn(origin, org.bukkit.entity.Mannequin.class, mannequin -> {
          mannequin.customName(definition.getName());
          mannequin.setDescription(definition.getDescription());

          var profile = Bukkit.createProfile(definition.getUuid(), null);
          profile.setProperty(new ProfileProperty(
              "textures", definition.getSkin().getData(), definition.getSkin().getSignature()));
          mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));

          mannequin.setSilent(definition.isSilent());
          mannequin.setInvulnerable(definition.isInvulnerable());
          mannequin.setGlowing(definition.getGlowing() != null);
          mannequin.setPose(toBukkitPose(definition.getPose()));
          mannequin.setImmovable(definition.isImmovable());
          mannequin.setMainHand(definition.getMainHand());
          mannequin.setGravity(definition.hasGravity());

          SkinPart.SkinLayers layers = definition.getLayers();
          SkinParts.Mutable parts = mannequin.getSkinParts();
          parts.setCapeEnabled(layers.contains(SkinPart.CAPE));
          parts.setJacketEnabled(layers.contains(SkinPart.JACKET));
          parts.setLeftSleeveEnabled(layers.contains(SkinPart.LEFT_SLEEVE));
          parts.setRightSleeveEnabled(layers.contains(SkinPart.RIGHT_SLEEVE));
          parts.setLeftPantsEnabled(layers.contains(SkinPart.LEFT_PANTS_LEG));
          parts.setRightPantsEnabled(layers.contains(SkinPart.RIGHT_PANTS_LEG));
          parts.setHatsEnabled(layers.contains(SkinPart.HAT));
          mannequin.setSkinParts(parts);
        });
    Mannequin wrapper = new Mannequin(entity, definition);
    entity.setMetadata(
        METADATA_KEY + definition.getId(), new FixedMetadataValue(PGM.get(), wrapper));
    return wrapper;
  }

  private static org.bukkit.entity.Pose toBukkitPose(MannequinPose pose) {
    return switch (pose) {
      case STANDING -> org.bukkit.entity.Pose.STANDING;
      case CROUCHING -> org.bukkit.entity.Pose.SNEAKING;
      case SWIMMING -> org.bukkit.entity.Pose.SWIMMING;
      case CRAWLING -> org.bukkit.entity.Pose.FALL_FLYING;
      case SLEEPING -> org.bukkit.entity.Pose.SLEEPING;
    };
  }

  public String getId() {
    return definition.getId();
  }

  public MannequinDefinition getDefinition() {
    return definition;
  }

  public UUID getEntityId() {
    return entity.getUniqueId();
  }

  public void setName(Component name) {
    entity.customName(name);
  }

  public void setDescription(Component description) {
    entity.setDescription(description);
  }

  public void setHideDescription(boolean hide) {
    entity.setDescription(null);
  }

  public void hideTitles() {
    entity.customName(null);
  }

  public void setHealth(float health) {
    entity.setHealth(health);
  }

  public void setSkin(UUID uuid, Skin skin) {
    var profile = Bukkit.createProfile(uuid, null);
    profile.setProperty(new ProfileProperty("textures", skin.getData(), skin.getSignature()));
    entity.setProfile(ResolvableProfile.resolvableProfile(profile));
  }

  public void setSkinLayers(SkinPart.SkinLayers layers) {
    SkinParts.Mutable parts = entity.getSkinParts();
    parts.setCapeEnabled(layers.contains(SkinPart.CAPE));
    parts.setJacketEnabled(layers.contains(SkinPart.JACKET));
    parts.setLeftSleeveEnabled(layers.contains(SkinPart.LEFT_SLEEVE));
    parts.setRightSleeveEnabled(layers.contains(SkinPart.RIGHT_SLEEVE));
    parts.setLeftPantsEnabled(layers.contains(SkinPart.LEFT_PANTS_LEG));
    parts.setRightPantsEnabled(layers.contains(SkinPart.RIGHT_PANTS_LEG));
    parts.setHatsEnabled(layers.contains(SkinPart.HAT));
    entity.setSkinParts(parts);
  }

  public void setPose(MannequinPose pose) {
    entity.setPose(toBukkitPose(pose));
  }

  public void teleport(double x, double y, double z, float yaw, float pitch) {
    entity.teleport(new Location(entity.getWorld(), x, y, z, yaw, pitch));
  }

  public float getYaw() {
    return entity.getLocation().getYaw();
  }

  public float getPitch() {
    return entity.getLocation().getPitch();
  }

  public void despawn() {
    entity.remove();
  }

  public void setGlowing(@Nullable TextColor color) {
    entity.setGlowing(color != null);
  }
}
