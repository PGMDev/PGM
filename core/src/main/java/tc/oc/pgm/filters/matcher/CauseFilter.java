package tc.oc.pgm.filters.matcher;

import static tc.oc.pgm.util.Assert.assertNotNull;

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
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.api.tracker.info.PotionInfo;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.ItemInfo;
import tc.oc.pgm.tracker.info.ProjectileInfo;
import tc.oc.pgm.util.event.GeneralizedEvent;
import tc.oc.pgm.util.event.PlayerPunchBlockEvent;
import tc.oc.pgm.util.event.PlayerTrampleBlockEvent;

public class CauseFilter extends TypedFilter.Impl<MatchQuery> {

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
    this.cause = assertNotNull(cause, "cause");
  }

  @Override
  public Class<? extends MatchQuery> queryType() {
    return MatchQuery.class;
  }

  @Override
  public boolean matches(MatchQuery query) {
    Event event = query.getEvent();
    // Prevent PlayerTrampleBlockEvent from casting, which will turn into PlayerCoarseMoveEvent
    // which doesn't work for trample
    if (event instanceof GeneralizedEvent && !(event instanceof PlayerTrampleBlockEvent)) {
      event = ((GeneralizedEvent) event).getCause();
    }

    EntityDamageEvent.DamageCause damageCause = null;
    DamageInfo damageInfo = null;
    boolean punchDamage = false;
    if (event instanceof EntityDamageEvent damageEvent) {
      damageCause = damageEvent.getCause();
      damageInfo = query.moduleRequire(TrackerMatchModule.class).resolveDamage(damageEvent);
      if (damageInfo instanceof MeleeInfo) {
        PhysicalInfo weapon = ((MeleeInfo) damageInfo).getWeapon();
        if (weapon instanceof ItemInfo && ((ItemInfo) weapon).getItem().getType() == Material.AIR) {
          punchDamage = true;
        }
      }
    }

    @Nullable Entity actor = GeneralizedEvent.getActorIfPresent(event);

    return switch (this.cause) {
      // Actor types
      case WORLD -> !(actor instanceof LivingEntity);
      case LIVING -> actor instanceof LivingEntity;
      case MOB -> actor instanceof LivingEntity && !(actor instanceof Player);
      case PLAYER -> actor instanceof Player;

      // Block actions
      case PUNCH -> event instanceof PlayerPunchBlockEvent || punchDamage;
      case TRAMPLE -> event instanceof PlayerTrampleBlockEvent;
      case MINE ->
        event instanceof BlockDamageEvent
            || event instanceof BlockBreakEvent
            || event instanceof PlayerBucketFillEvent;

      // Damage types
      case MELEE ->
        damageCause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
            || damageInfo instanceof MeleeInfo;
      case PROJECTILE ->
        damageCause == EntityDamageEvent.DamageCause.PROJECTILE
            || damageInfo instanceof ProjectileInfo;
      case POTION ->
        damageCause == EntityDamageEvent.DamageCause.MAGIC
            || damageCause == EntityDamageEvent.DamageCause.POISON
            || damageCause == EntityDamageEvent.DamageCause.WITHER
            || damageInfo instanceof PotionInfo;
      case EXPLOSION ->
        event instanceof EntityExplodeEvent
            || damageCause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
            || damageCause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
      case COMBUSTION ->
        event instanceof BlockBurnEvent
            || damageCause == EntityDamageEvent.DamageCause.FIRE
            || damageCause == EntityDamageEvent.DamageCause.FIRE_TICK
            || damageCause == EntityDamageEvent.DamageCause.LAVA;
      case FALL -> // Strictly damage from hitting the ground
        damageCause == EntityDamageEvent.DamageCause.FALL;
      case GRAVITY -> // Any damage caused by a fall
        damageCause == EntityDamageEvent.DamageCause.FALL
            || damageCause == EntityDamageEvent.DamageCause.VOID;
      case VOID -> damageCause == EntityDamageEvent.DamageCause.VOID;
      case SQUASH -> damageCause == EntityDamageEvent.DamageCause.FALLING_BLOCK;
      case SUFFOCATION -> damageCause == EntityDamageEvent.DamageCause.SUFFOCATION;
      case DROWNING -> damageCause == EntityDamageEvent.DamageCause.DROWNING;
      case STARVATION -> damageCause == EntityDamageEvent.DamageCause.STARVATION;
      case LIGHTNING -> damageCause == EntityDamageEvent.DamageCause.LIGHTNING;
      case CACTUS -> damageCause == EntityDamageEvent.DamageCause.CONTACT;
      case THORNS -> damageCause == EntityDamageEvent.DamageCause.THORNS;
    };
  }
}
