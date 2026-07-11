package tc.oc.pgm.platform.modern.modules.mannequin;

import com.destroystokyo.paper.SkinParts;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.metadata.FixedMetadataValue;
import tc.oc.pgm.api.PGM;

public class Mannequin {

  public static final String METADATA_KEY = "PGM_mannequin_";
  private final org.bukkit.entity.Mannequin entity;
  private final String id;

  public Mannequin(org.bukkit.entity.Mannequin entity, String id) {
    this.entity = entity;
    this.id = id;
  }

  public static Mannequin spawn(String id, Location origin, MannequinDefinition definition) {
    org.bukkit.entity.Mannequin entity = origin
        .getWorld()
        .spawn(origin, org.bukkit.entity.Mannequin.class, mannequin -> {
          mannequin.setCustomName(definition.getName());

          var profile = Bukkit.createProfile(definition.getUuid(), null);
          profile.setProperty(new ProfileProperty(
              "textures", definition.getSkin().getData(), definition.getSkin().getSignature()));
          mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));

          mannequin.setSilent(definition.isSilent());
          mannequin.setInvulnerable(definition.isInvulnerable());
          mannequin.setGlowing(definition.getGlowing() != null);
          mannequin.setPose(toBukkitPose(definition.getPose()));
          mannequin.setImmovable(definition.isImmovable());

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
    Mannequin wrapper = new Mannequin(entity, id);
    entity.setMetadata(METADATA_KEY + id, new FixedMetadataValue(PGM.get(), wrapper));
    return wrapper;
  }

  private static org.bukkit.entity.Pose toBukkitPose(MannequinPose pose) {
    return switch (pose) {
      case STANDING -> org.bukkit.entity.Pose.STANDING;
      case CROUCHING -> org.bukkit.entity.Pose.SNEAKING;
      case SWIMMING -> org.bukkit.entity.Pose.SWIMMING;
      case GLIDING -> org.bukkit.entity.Pose.FALL_FLYING;
      case SLEEPING -> org.bukkit.entity.Pose.SLEEPING;
    };
  }

  public String getId() {
    return this.id;
  }

  public void teleport(Location location) {
    entity.teleport(location);
  }

  public void despawn() {
    entity.remove();
  }

  public void setPose(MannequinPose pose) {
    entity.setPose(toBukkitPose(pose));
  }

  public void setGlowing(@Nullable Color color) {
    entity.setGlowing(color != null);
  }
}
