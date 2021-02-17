package tc.oc.pgm.filters;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.tracker.info.*;
import tc.oc.pgm.api.tracker.info.PotionInfo;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.ItemInfo;
import tc.oc.pgm.tracker.info.ProjectileInfo;
import tc.oc.pgm.util.event.GeneralizedEvent;
import tc.oc.pgm.util.event.PlayerPunchBlockEvent;
import tc.oc.pgm.util.event.PlayerTrampleBlockEvent;

public class CauseFilter extends TypedFilter<MatchQuery> {

  // TODO: add other causes like growth, flow, mob, machine, etc.
  public enum Cause {
    // Actor types
    WORLD,
    LIVING,
    MOB,
    PLAYER,

    // Block actions
    PUNCH,
    TRAMPLE,
    MINE,

    // Damage types
    MELEE,
    PROJECTILE,
    POTION,
    EXPLOSION,
    COMBUSTION,
    FALL,
    GRAVITY,
    VOID,
    SQUASH,
    SUFFOCATION,
    DROWNING,
    STARVATION,
    LIGHTNING,
    CACTUS,
    THORNS,
  }

  private final Cause cause;

  public CauseFilter(Cause cause) {
    Preconditions.checkNotNull(cause);
    this.cause = cause;
  }

  @Override
  public Class<? extends MatchQuery> getQueryType() {
    return MatchQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(MatchQuery query) {
    return QueryResponse.fromBoolean(matches(query));
  }

  private boolean matches(MatchQuery query) {
    Event event = query.getEvent();
    if (event instanceof GeneralizedEvent) {
      event = ((GeneralizedEvent) event).getCause();
    }

    TrackerMatchModule tracker = query.getMatch().needModule(TrackerMatchModule.class);

    EntityDamageEvent.DamageCause damageCause = null;
    DamageInfo damageInfo = null;
    boolean punchDamage = false;
    if (event instanceof EntityDamageEvent) {
      EntityDamageEvent damageEvent = (EntityDamageEvent) event;
      damageCause = damageEvent.getCause();
      damageInfo = tracker.resolveDamage(damageEvent);
      if (damageInfo instanceof MeleeInfo) {
        PhysicalInfo weapon = ((MeleeInfo) damageInfo).getWeapon();
        if (weapon instanceof ItemInfo && ((ItemInfo) weapon).getItem().getType() == Material.AIR) {
          punchDamage = true;
        }
      }
    }

    @Nullable Entity actor = GeneralizedEvent.getActorIfPresent(event);

    switch (this.cause) {
        // Actor types
      case WORLD:
        return !(actor instanceof LivingEntity);

      case LIVING:
        return actor instanceof LivingEntity;

      case MOB:
        return actor instanceof LivingEntity && !(actor instanceof Player);

      case PLAYER:
        return actor instanceof Player;

        // Block actions
      case PUNCH:
        return event instanceof PlayerPunchBlockEvent || punchDamage;

      case TRAMPLE:
        return event instanceof PlayerTrampleBlockEvent;

      case MINE:
        return event instanceof BlockDamageEvent
            || event instanceof BlockBreakEvent
            || event instanceof PlayerBucketFillEvent;

        // Damage types
      case MELEE:
        return damageCause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
            || damageInfo instanceof MeleeInfo;

      case PROJECTILE:
        return damageCause == EntityDamageEvent.DamageCause.PROJECTILE
            || damageInfo instanceof ProjectileInfo;

      case POTION:
        return damageCause == EntityDamageEvent.DamageCause.MAGIC
            || damageCause == EntityDamageEvent.DamageCause.POISON
            || damageCause == EntityDamageEvent.DamageCause.WITHER
            || damageInfo instanceof PotionInfo;

      case EXPLOSION:
        return event instanceof EntityExplodeEvent
            || damageCause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
            || damageCause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;

      case COMBUSTION:
        return event instanceof BlockBurnEvent
            || damageCause == EntityDamageEvent.DamageCause.FIRE
            || damageCause == EntityDamageEvent.DamageCause.FIRE_TICK
            || damageCause == EntityDamageEvent.DamageCause.LAVA;

      case FALL: // Strictly damage from hitting the ground
        return damageCause == EntityDamageEvent.DamageCause.FALL;

      case GRAVITY: // Any damage caused by a fall
        return damageCause == EntityDamageEvent.DamageCause.FALL
            || damageCause == EntityDamageEvent.DamageCause.VOID;

      case VOID:
        return damageCause == EntityDamageEvent.DamageCause.VOID;

      case SQUASH:
        return damageCause == EntityDamageEvent.DamageCause.FALLING_BLOCK;

      case SUFFOCATION:
        return damageCause == EntityDamageEvent.DamageCause.SUFFOCATION;

      case DROWNING:
        return damageCause == EntityDamageEvent.DamageCause.DROWNING;

      case STARVATION:
        return damageCause == EntityDamageEvent.DamageCause.STARVATION;

      case LIGHTNING:
        return damageCause == EntityDamageEvent.DamageCause.LIGHTNING;

      case CACTUS:
        return damageCause == EntityDamageEvent.DamageCause.CONTACT;

      case THORNS:
        return damageCause == EntityDamageEvent.DamageCause.THORNS;

      default:
        return false;
    }
  }
}
