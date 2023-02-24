package tc.oc.pgm.modules;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;

import java.util.Set;
import net.kyori.adventure.sound.Sound;
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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.query.PlayerQuery;
import tc.oc.pgm.projectile.EntityLaunchEvent;
import tc.oc.pgm.util.bukkit.MetadataUtils;
import tc.oc.pgm.util.nms.NMSHacks;

@ListenerScope(MatchScope.RUNNING)
public class ModifyBowProjectileMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Class<? extends Entity> cls;
  private final float velocityMod;
  private final Set<PotionEffect> potionEffects;
  private final Filter pickupFilter;

  private static final Sound PROJECTILE_SOUND =
      sound(key("random.successful_hit"), Sound.Source.MASTER, 0.18f, 0.45f);

  public ModifyBowProjectileMatchModule(
      Match match,
      Class<? extends Entity> cls,
      float velocityMod,
      Set<PotionEffect> effects,
      Filter pickupFilter) {
    this.match = match;
    this.cls = cls;
    this.velocityMod = velocityMod;
    potionEffects = effects;
    this.pickupFilter = pickupFilter;
  }

  @EventHandler(ignoreCancelled = true)
  public void changeBowProjectile(EntityShootBowEvent event) {
    Plugin plugin = PGM.get();
    Entity newProjectile;

    if (this.cls == Arrow.class && event.getProjectile() instanceof Arrow) {
      // Don't change the projectile if it's an Arrow and the custom entity type is also Arrow
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
      if (oldEntity instanceof Arrow) {
        Arrow arrow = (Arrow) oldEntity;
        newProjectile.setMetadata("critical", new FixedMetadataValue(plugin, arrow.isCritical()));
        newProjectile.setMetadata(
            "knockback", new FixedMetadataValue(plugin, arrow.getKnockbackStrength()));
        newProjectile.setMetadata(
            "damage", new FixedMetadataValue(plugin, arrow.spigot().getDamage()));
      }
    }

    // Tag the projectile as custom
    newProjectile.setMetadata("customProjectile", new FixedMetadataValue(plugin, true));

    match.callEvent(new EntityLaunchEvent(newProjectile, event.getEntity()));
  }

  @EventHandler(ignoreCancelled = true)
  public void fixEntityDamage(EntityDamageByEntityEvent event) {
    Entity projectile = event.getDamager();
    if (projectile.hasMetadata("customProjectile")) {

      // If the custom projectile replaced an arrow, recreate some effects specific to arrows
      if (projectile.hasMetadata("damage")) {
        boolean critical = MetadataUtils.getMetadata(projectile, "critical", PGM.get()).asBoolean();
        int knockback = MetadataUtils.getMetadata(projectile, "knockback", PGM.get()).asInt();
        double damage = MetadataUtils.getMetadata(projectile, "damage", PGM.get()).asDouble();
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
          double horizontalSpeed =
              Math.sqrt(
                  projectileVelocity.getX() * projectileVelocity.getX()
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
            && (projectile instanceof Projectile && !(projectile instanceof Arrow))) {
          Projectile customProjectile = (Projectile) projectile;
          if (customProjectile.getShooter() instanceof Player) {
            Player bukkitShooter = (Player) customProjectile.getShooter();
            MatchPlayer shooter = match.getPlayer(bukkitShooter);
            if (shooter != null && event.getEntity() != null) {
              shooter.playSound(PROJECTILE_SOUND);
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
    if (!NMSHacks.isCraftItemArrowEntity(event.getItem())) {
      return;
    }
    Filter.QueryResponse response =
        pickupFilter.query(new PlayerQuery(event, match.getPlayer(event.getPlayer())));
    if (response.isDenied()) {
      event.setCancelled(true);
    }
  }
}
