package tc.oc.pgm.tracker.damage;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.bukkit.Location;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedTranslatable;
import tc.oc.util.components.Components;

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
  public Component getLocalizedName() {
    if (customName != null) {
      return Components.fromLegacyText(customName);
    } else if (getProjectile() instanceof PotionInfo) {
      // PotionInfo.getLocalizedName returns a potion name,
      // which doesn't work outside a potion death message.
      return new PersonalizedTranslatable("item.potion.name");
    } else {
      return getProjectile().getLocalizedName();
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
