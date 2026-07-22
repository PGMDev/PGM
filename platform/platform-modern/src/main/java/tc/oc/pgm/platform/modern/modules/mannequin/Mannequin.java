package tc.oc.pgm.platform.modern.modules.mannequin;

import com.destroystokyo.paper.SkinParts;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.skin.Skin;

public class Mannequin {

  private final org.bukkit.entity.Mannequin entity;
  private final MannequinDefinition definition;
  private final Vector spawnPos;

  public Mannequin(org.bukkit.entity.Mannequin entity, MannequinDefinition definition) {
    this.entity = entity;
    this.definition = definition;
    this.spawnPos = entity.getLocation().toVector();
  }

  public static Mannequin spawn(
      Location origin,
      MannequinDefinition definition,
      @Nullable UUID uuidOverride,
      @Nullable Skin skinOverride) {
    org.bukkit.entity.Mannequin entity = origin
        .getWorld()
        .spawn(origin, org.bukkit.entity.Mannequin.class, mannequin -> {
          mannequin.customName(definition.getName());
          mannequin.setDescription(definition.getDescription());
          mannequin.setSilent(definition.isSilent());
          mannequin.setInvulnerable(definition.isInvulnerable());
          mannequin.setGlowing(definition.isGlowing());
          mannequin.setHealth(definition.getHealth());
          mannequin.setPose(toBukkitPose(definition.getPose()));
          mannequin.setImmovable(definition.isImmovable());
          mannequin.setMainHand(definition.getMainHand());
          mannequin.setGravity(definition.hasGravity());
          mannequin.setNoPhysics(!definition.hasPhysics());
          mannequin.setVisualFire(definition.isOnFire() ? TriState.TRUE : TriState.FALSE);
        });
    Mannequin wrapper = new Mannequin(entity, definition);

    UUID uuid = uuidOverride != null ? uuidOverride : definition.getUuid();
    Skin skin = skinOverride != null ? skinOverride : definition.getSkin();
    if (uuid != null && skin != null) {
      wrapper.setSkin(uuid, skin);
    }
    wrapper.setSkinLayers(definition.getLayers());

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

  public void setHideDescription() {
    entity.setDescription(null);
  }

  public void hideTitles() {
    entity.customName(null);
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

  public void setSilent(boolean silent) {
    entity.setSilent(silent);
  }

  public void setInvulnerable(boolean invulnerable) {
    entity.setInvulnerable(invulnerable);
  }

  public void setGlowing(boolean glowing) {
    entity.setGlowing(glowing);
  }

  public void setHealth(float health) {
    entity.registerAttribute(Attribute.MAX_HEALTH);
    var maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);

    if (maxHealth != null) {
      maxHealth.setBaseValue(health);
    }

    entity.setHealth(health);
  }

  public void setImmovable(boolean immovable) {
    entity.setImmovable(immovable);
  }

  public void setGravity(boolean gravity) {
    entity.setGravity(gravity);
  }

  public void setPhysics(boolean physics) {
    entity.setNoPhysics(!physics);
  }

  public void setOnFire(boolean onFire) {
    entity.setVisualFire(onFire ? TriState.TRUE : TriState.FALSE);
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

  public Location getLocation() {
    return entity.getLocation();
  }

  public org.bukkit.entity.Mannequin getEntity() {
    return entity;
  }

  public Vector getSpawnPos() {
    return spawnPos;
  }
}
