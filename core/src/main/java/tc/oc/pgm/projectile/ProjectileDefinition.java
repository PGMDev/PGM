package tc.oc.pgm.projectile;

import java.time.Duration;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

public class ProjectileDefinition extends SelfIdentifyingFeatureDefinition {
  protected @Nullable String name;
  protected @Nullable Double damage;
  protected double velocity;
  protected ClickAction clickAction;
  protected Class<? extends Entity> projectile;
  protected List<PotionEffect> potion;
  protected Filter destroyFilter;
  protected Duration coolDown;
  protected boolean throwable;
  protected boolean precise;

  public ProjectileDefinition(
      @Nullable String id,
      @Nullable String name,
      @Nullable Double damage,
      double velocity,
      ClickAction clickAction,
      Class<? extends Entity> entity,
      List<PotionEffect> potion,
      Filter destroyFilter,
      Duration coolDown,
      boolean throwable,
      boolean precise) {
    super(id);
    this.name = name;
    this.damage = damage;
    this.velocity = velocity;
    this.clickAction = clickAction;
    this.projectile = entity;
    this.potion = potion;
    this.destroyFilter = destroyFilter;
    this.coolDown = coolDown;
    this.throwable = throwable;
    this.precise = precise;
  }

  public @Nullable String getName() {
    return name;
  }
}
