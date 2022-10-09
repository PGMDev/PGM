package tc.oc.pgm.tracker.info;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.Assert.assertNotNull;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
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
    this.projectile = assertNotNull(projectile);
    this.shooter = shooter;
    this.origin = assertNotNull(origin);
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
  public net.kyori.adventure.text.Component getName() {
    if (customName != null) {
      return LegacyComponentSerializer.legacySection().deserialize(customName);
    } else if (getProjectile() instanceof PotionInfo) {
      // PotionInfo.getName returns a potion name,
      // which doesn't work outside a potion death message.
      return translatable("item.potion.name");
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
