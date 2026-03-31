package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.api.tracker.info.PotionInfo;
import tc.oc.pgm.api.tracker.info.RangedInfo;
import tc.oc.pgm.util.text.MinecraftComponent;

public record ProjectileInfo(
    PhysicalInfo projectile,
    @Nullable PhysicalInfo shooter,
    Location origin,
    @Nullable String customName)
    implements PhysicalInfo, DamageInfo, RangedInfo {

  public ProjectileInfo {
    assertNotNull(projectile);
    assertNotNull(origin);
  }

  @Override
  public @Nullable PhysicalInfo damager() {
    return projectile();
  }

  @Deprecated
  public PhysicalInfo getProjectile() {
    return projectile();
  }

  @Deprecated
  public @Nullable PhysicalInfo getShooter() {
    return shooter();
  }

  @Override
  public @Nullable ParticipantState owner() {
    return shooter == null ? null : shooter.owner();
  }

  @Override
  public @Nullable ParticipantState attacker() {
    return owner();
  }

  @Override
  public String identifier() {
    return projectile().identifier();
  }

  @Override
  public Component name() {
    String customName = customName();
    if (customName != null) {
      return LegacyComponentSerializer.legacySection().deserialize(customName);
    } else if (projectile() instanceof PotionInfo) {
      // PotionInfo.name returns a potion name,
      // which doesn't work outside a potion death message.
      return MinecraftComponent.material(Material.POTION);
    } else {
      return projectile().name();
    }
  }

  @Override
  public @NonNull String toString() {
    return getClass().getSimpleName()
        + "{projectile="
        + projectile()
        + " origin="
        + origin()
        + " shooter="
        + shooter()
        + "}";
  }
}
