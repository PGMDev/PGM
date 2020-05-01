package tc.oc.pgm.tracker.info;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.api.tracker.info.PotionInfo;
import tc.oc.pgm.api.tracker.info.RangedInfo;

public class ProjectileInfo implements PhysicalInfo, DamageInfo, RangedInfo {

  private final PhysicalInfo projectile;
  private final @Nullable PhysicalInfo shooter;
  private final Location origin;
  private final @Nullable String customName;

  public ProjectileInfo(
      PhysicalInfo projectile,
      @Nullable PhysicalInfo shooter,
      Location origin,
      @Nullable String customName) {
    this.projectile = checkNotNull(projectile);
    this.shooter = shooter;
    this.origin = checkNotNull(origin);
    this.customName = customName;
  }

  public PhysicalInfo getProjectile() {
    return projectile;
  }

  public @Nullable PhysicalInfo getShooter() {
    return shooter;
  }

  @Override
  public Location getOrigin() {
    return this.origin;
  }

  @Override
  public @Nullable ParticipantState getOwner() {
    return shooter == null ? null : shooter.getOwner();
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return getOwner();
  }

  @Override
  public String getIdentifier() {
    return getProjectile().getIdentifier();
  }

  @Override
  public net.kyori.text.Component getName() {
    if (customName != null) {
      return LegacyComponentSerializer.legacy().deserialize(customName);
    } else if (getProjectile() instanceof PotionInfo) {
      // PotionInfo.getName returns a potion name,
      // which doesn't work outside a potion death message.
      return TranslatableComponent.of("item.potion.name");
    } else {
      return getProjectile().getName();
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{projectile="
        + getProjectile()
        + " origin="
        + getOrigin()
        + " shooter="
        + getShooter()
        + "}";
  }
}
