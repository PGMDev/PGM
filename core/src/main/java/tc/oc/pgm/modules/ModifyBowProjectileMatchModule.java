package tc.oc.pgm.modules;

import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;
import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import java.util.Set;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.query.PlayerQuery;
import tc.oc.pgm.projectile.EntityLaunchEvent;
import tc.oc.pgm.util.EntityMap;
import tc.oc.pgm.util.EntitySet;
import tc.oc.pgm.util.bukkit.Sounds;

@ListenerScope(MatchScope.RUNNING)
public class ModifyBowProjectileMatchModule implements MatchModule, Listener {

  private final Match match;
  private final @Nullable Class<? extends Entity> cls;
  private final float velocityMod;
  private final Set<PotionEffect> potionEffects;
  private final Filter pickupFilter;
  private final EntitySet customProjectiles;
  private final EntityMap<ArrowProperties> replacedArrows;

  public ModifyBowProjectileMatchModule(
      Match match,
      @Nullable Class<? extends Entity> cls,
      float velocityMod,
      Set<PotionEffect> effects,
      Filter pickupFilter) {
    this.match = match;
    this.cls = cls;
    this.velocityMod = velocityMod;
    this.potionEffects = effects;
    this.pickupFilter = pickupFilter;
    this.customProjectiles = new EntitySet(match);
    this.replacedArrows = new EntityMap<>(match);
  }

  @EventHandler(ignoreCancelled = true)
  public void changeBowProjectile(EntityShootBowEvent event) {
    Entity newProjectile;

    if (this.cls == null) {
      // Don't replace the projectile, keep the original
      newProjectile = event.getProjectile();
    } else {
      // Replace the projectile
      Projectile oldEntity = (Projectile) event.getProjectile();
      if (Projectile.class.isAssignableFrom(this.cls)) {
        newProjectile = event.getEntity().launchProjectile((Class<? extends Projectile>) this.cls);
      } else {
        World world = event.getEntity().getWorld();
        newProjectile = world.spawn(oldEntity.getLocation(), this.cls);
      }
      event.setProjectile(newProjectile);

      // Copy some things from the old projectile
      newProjectile.setVelocity(oldEntity.getVelocity());
      newProjectile.setFallDistance(oldEntity.getFallDistance());
      newProjectile.setFireTicks(oldEntity.getFireTicks());

      if (newProjectile instanceof Projectile) {
        ((Projectile) newProjectile).setShooter(oldEntity.getShooter());
        ((Projectile) newProjectile).setBounce(oldEntity.doesBounce());
      }

      // Save some special properties of Arrows
      if (oldEntity instanceof Arrow arrow) {
        replacedArrows.put(
            newProjectile,
            new ArrowProperties(
                arrow.isCritical(),
                arrow.getKnockbackStrength(),
                MISC_UTILS.getArrowDamage(arrow)));
      }
    }

    customProjectiles.add(newProjectile);

    match.callEvent(new EntityLaunchEvent(newProjectile, event.getEntity()));
  }

  @EventHandler(ignoreCancelled = true)
  public void fixEntityDamage(EntityDamageByEntityEvent event) {
    Entity projectile = event.getDamager();
    if (customProjectiles.contains(projectile)) {

      // If the custom projectile replaced an arrow, recreate some effects specific to arrows
      ArrowProperties arrow = replacedArrows.get(projectile);
      if (arrow != null) {
        boolean critical = arrow.critical();
        int knockback = arrow.knockback();
        double damage = arrow.damage();
        double speed = projectile.getVelocity().length();

        // Reproduce the damage calculation from nms.EntityArrow with the addition of our modifier
        int finalDamage = (int) Math.ceil(speed * damage * this.velocityMod);
        if (critical) {
          finalDamage += match.getRandom().nextInt(finalDamage / 2 + 2);
        }
        event.setDamage(finalDamage);

        // Flame arrows - target burns for 5 seconds always
        if (projectile.getFireTicks() > 0) {
          event.getEntity().setFireTicks(100);
        }

        // Reproduce the knockback calculation for punch bows
        if (knockback > 0) {
          Vector projectileVelocity = projectile.getVelocity();
          double horizontalSpeed = Math.sqrt(projectileVelocity.getX() * projectileVelocity.getX()
              + projectileVelocity.getZ() * projectileVelocity.getZ());
          Vector velocity = event.getEntity().getVelocity();
          velocity.setX(
              velocity.getX() + projectileVelocity.getX() * knockback * 0.6 / horizontalSpeed);
          velocity.setY(velocity.getY() + 0.1);
          velocity.setZ(
              velocity.getZ() + projectileVelocity.getZ() * knockback * 0.6 / horizontalSpeed);
          event.getEntity().setVelocity(velocity);
        }

        // If the projectile is not an arrow, play an impact sound.
        if (event.getEntity() instanceof Player
            && (projectile instanceof Projectile customProjectile
                && !(projectile instanceof Arrow))) {
          if (customProjectile.getShooter() instanceof Player bukkitShooter) {
            MatchPlayer shooter = match.getPlayer(bukkitShooter);
            if (shooter != null && event.getEntity() != null) {
              shooter.playSound(Sounds.PROJECTILE_HIT);
            }
          }
        }
      }

      // Apply any potion effects attached to the projectile
      if (event.getEntity() instanceof LivingEntity) {
        for (PotionEffect potionEffect : this.potionEffects) {
          ((LivingEntity) event.getEntity()).addPotionEffect(potionEffect);
        }
      }
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void preventArrowPickup(PlayerPickupItemEvent event) {
    if (!NMS_HACKS.isCraftItemArrowEntity(event)) {
      return;
    }
    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null) return;
    Filter.QueryResponse response = pickupFilter.query(new PlayerQuery(event, player));
    if (response.isDenied()) {
      event.setCancelled(true);
    }
  }

  private record ArrowProperties(boolean critical, int knockback, double damage) {}
}
