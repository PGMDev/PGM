package tc.oc.pgm.projectile;

import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.joda.time.Duration;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.filters.Filter;

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
      boolean throwable) {
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
  }

  public @Nullable String getName() {
    return name;
  }
}
