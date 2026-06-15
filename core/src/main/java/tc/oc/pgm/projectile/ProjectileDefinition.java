package tc.oc.pgm.projectile;

import java.time.Duration;
import java.util.List;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.projectile.projectiles.PgmProjectile;

public class ProjectileDefinition extends SelfIdentifyingFeatureDefinition {
  protected final @Nullable String name;
  protected final @Nullable Double damage;
  protected final double velocity;
  protected final ClickAction clickAction;
  protected final PgmProjectile projectile;
  protected final List<PotionEffect> potion;
  protected final Filter destroyFilter;
  protected final Duration coolDown;
  protected final boolean throwable;

  public ProjectileDefinition(
      @Nullable String id,
      @Nullable String name,
      @Nullable Double damage,
      double velocity,
      ClickAction clickAction,
      PgmProjectile projectile,
      List<PotionEffect> potion,
      Filter destroyFilter,
      Duration coolDown,
      boolean throwable) {
    super(id);
    this.name = name;
    this.damage = damage;
    this.velocity = velocity;
    this.clickAction = clickAction;
    this.projectile = projectile;
    this.potion = potion;
    this.destroyFilter = destroyFilter;
    this.coolDown = coolDown;
    this.throwable = throwable;
  }

  public @Nullable String getName() {
    return name;
  }
}
