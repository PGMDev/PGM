package tc.oc.pgm.damage;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.DamageQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.FallInfo;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.BlockInfo;
import tc.oc.pgm.tracker.info.EntityInfo;
import tc.oc.pgm.tracker.info.ExplosionInfo;
import tc.oc.pgm.tracker.info.FireInfo;
import tc.oc.pgm.tracker.info.GenericDamageInfo;
import tc.oc.pgm.tracker.info.ProjectileInfo;
import tc.oc.pgm.util.inventory.PotionClassifier;

@ListenerScope(MatchScope.RUNNING)
public class DamageMatchModule implements MatchModule, Listener {

  private final Match match;
  private final List<Filter> filters;

  public DamageMatchModule(Match match, List<Filter> filters) {
    this.match = match;
    this.filters = filters;
  }

  TrackerMatchModule tracker() {
    return match.needModule(TrackerMatchModule.class);
  }

  /**
   * Are players allowed to inflict the given damage on themselves? Custom rules can override this.
   */
  public static boolean isAllowedSelfDamage(DamageInfo damageInfo) {
    // Disable self-damage with arrows
    if (damageInfo instanceof ProjectileInfo) {
      ProjectileInfo projectileInfo = (ProjectileInfo) damageInfo;
      if (projectileInfo.getProjectile() instanceof EntityInfo
          && ((EntityInfo) projectileInfo.getProjectile()).getEntityType() == EntityType.ARROW) {
        return false;
      }
    }
    return true;
  }

  /**
   * Is the given damage of a type that is always allowed against teammates, even if friendly fire
   * is disabled? Custom rules can override this.
   */
  public static boolean isAllowedTeamDamage(DamageInfo damageInfo) {
    return damageInfo instanceof ExplosionInfo
        || damageInfo instanceof FireInfo
        || damageInfo instanceof FallInfo
        || damageInfo instanceof BlockInfo
        || damageInfo
            instanceof
            GenericDamageInfo; // This should never have an attacker anyway, but just in case
  }

  /** Test if the given damage/attack is allowed by the default damage policies */
  public Filter.QueryResponse queryDefaultRules(ParticipantState victim, DamageInfo damageInfo) {
    switch (PlayerRelation.get(victim, damageInfo.getAttacker())) {
      case SELF:
        if (!isAllowedSelfDamage(damageInfo)) {
          return Filter.QueryResponse.DENY;
        }

      case ALLY:
        if (!match.getFriendlyFire() && !isAllowedTeamDamage(damageInfo)) {
          return Filter.QueryResponse.DENY;
        }

      default:
        return Filter.QueryResponse.ABSTAIN;
    }
  }

  /** Get a query for the given damage event */
  public DamageQuery getQuery(
      @Nullable Cancellable event, ParticipantState victim, DamageInfo damageInfo) {
    return tc.oc.pgm.filters.query.DamageQuery.victimDefault((Event) event, victim, damageInfo);
  }

  /** Query the custom damage filters with the given damage event */
  public Filter.QueryResponse queryRules(
      @Nullable Cancellable event, ParticipantState victim, DamageInfo damageInfo) {
    Query query = getQuery(event, victim, damageInfo);

    for (Filter filter : filters) {
      Filter.QueryResponse response = filter.query(query);
      if (response != Filter.QueryResponse.ABSTAIN) return response;
    }

    return Filter.QueryResponse.ABSTAIN;
  }

  /** Query whether the given damage is both allowed and incentivized for the attacker. */
  public Filter.QueryResponse queryHostile(ParticipantState victim, DamageInfo damageInfo) {
    switch (PlayerRelation.get(victim, damageInfo.getAttacker())) {
      case SELF:
      case ALLY:
        // Players don't want to hurt themselves or their teammates
        return Filter.QueryResponse.DENY;

      default:
        // They also don't want to waste time trying to inflict damage that will be filtered out
        return queryRules(null, victim, damageInfo);
    }
  }

  /**
   * Query whether the given damage is allowed or not. Both the custom damage filters and the
   * friendly fire policy are considered, the former having priority over the latter.
   */
  public Filter.QueryResponse queryDamage(
      @Nullable Cancellable event, ParticipantState victim, DamageInfo damageInfo) {
    Filter.QueryResponse response = queryRules(event, victim, damageInfo);
    if (response != Filter.QueryResponse.ABSTAIN) return response;

    return queryDefaultRules(victim, damageInfo);
  }

  /** Query the given damage event and cancel it if the result was denied. */
  public Filter.QueryResponse processDamageEvent(
      Cancellable event, ParticipantState victim, DamageInfo damageInfo) {
    Filter.QueryResponse response = queryDamage(assertNotNull(event), victim, damageInfo);
    if (response.isDenied()) {
      event.setCancelled(true);
    }
    return response;
  }

  /** Search the rider stack for a participant */
  @Nullable
  MatchPlayer getVictim(Entity entity) {
    if (entity == null) return null;

    MatchPlayer victim = match.getParticipant(entity);
    if (victim != null) {
      return victim;
    } else {
      return getVictim(entity.getPassenger());
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    MatchPlayer victim = getVictim(event.getEntity());
    if (victim == null) return;

    processDamageEvent(event, victim.getParticipantState(), tracker().resolveDamage(event));
  }

  @EventHandler(ignoreCancelled = true)
  public void onDamageVehicle(VehicleDamageEvent event) {
    MatchPlayer victim = getVictim(event.getVehicle());
    if (victim == null) return;

    processDamageEvent(
        event,
        victim.getParticipantState(),
        tracker()
            .resolveDamage(
                EntityDamageEvent.DamageCause.CUSTOM, event.getVehicle(), event.getAttacker()));
  }

  @EventHandler(ignoreCancelled = true)
  public void onIgnition(EntityCombustByEntityEvent event) {
    MatchPlayer victim = getVictim(event.getEntity());
    if (victim == null) return;

    processDamageEvent(
        event,
        victim.getParticipantState(),
        tracker()
            .resolveDamage(
                EntityDamageEvent.DamageCause.FIRE, event.getEntity(), event.getCombuster()));
  }

  @EventHandler(ignoreCancelled = true)
  public void onIgnition(EntityCombustByBlockEvent event) {
    MatchPlayer victim = getVictim(event.getEntity());
    if (victim == null) return;

    processDamageEvent(
        event,
        victim.getParticipantState(),
        tracker()
            .resolveDamage(
                EntityDamageEvent.DamageCause.FIRE, event.getEntity(), event.getCombuster()));
  }

  @EventHandler(ignoreCancelled = true)
  public void onPotionSplash(final PotionSplashEvent event) {
    ThrownPotion potion = event.getPotion();
    if (!PotionClassifier.isHarmful(potion)) return;

    for (LivingEntity entity : event.getAffectedEntities()) {
      ParticipantState victim = match.getParticipantState(entity);
      DamageInfo damageInfo =
          tracker().resolveDamage(EntityDamageEvent.DamageCause.MAGIC, entity, potion);

      if (victim != null && queryDamage(event, victim, damageInfo).isDenied()) {
        event.setIntensity(entity, 0);
      }
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onTarget(EntityTargetEvent event) {
    if (!(event.getEntity() instanceof ExperienceOrb)) {
      ParticipantState victimState = null;
      if (event.getTarget() instanceof Player) {
        // Don't target allies
        MatchPlayer victim = getVictim(event.getTarget());
        if (victim == null) return;
        victimState = victim.getParticipantState();
      } else if (event.getTarget() != null) {
        // Don't target other mobs owned by allies
        victimState = tracker().getOwner(event.getTarget());
      }
      if (victimState == null) return;

      DamageInfo damageInfo =
          tracker()
              .resolveDamage(
                  EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                  event.getTarget(),
                  event.getEntity());
      if (queryHostile(victimState, damageInfo).isDenied()) {
        event.setCancelled(true);
      }
    }
  }
}
