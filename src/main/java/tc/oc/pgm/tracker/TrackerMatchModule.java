package tc.oc.pgm.tracker;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.tracker.damage.DamageInfo;
import tc.oc.pgm.tracker.damage.NullDamageInfo;
import tc.oc.pgm.tracker.damage.PhysicalInfo;
import tc.oc.pgm.tracker.damage.TrackerInfo;
import tc.oc.pgm.tracker.resolvers.*;
import tc.oc.pgm.tracker.trackers.*;

public class TrackerMatchModule extends MatchModule {

  private final EntityTracker entityTracker = new EntityTracker(this);
  private final BlockTracker blockTracker = new BlockTracker(this);
  private final FallTracker fallTracker = new FallTracker(this);
  private final FireTracker fireTracker = new FireTracker(this);

  private final Set<DamageResolver> damageResolvers = new LinkedHashSet<>();

  public TrackerMatchModule(Match match) {
    super(match);

    // Damage resolvers - order is important!
    damageResolvers.add(fallTracker);
    damageResolvers.add(fireTracker);
    damageResolvers.add(new PotionDamageResolver());
    damageResolvers.add(new ExplosionDamageResolver());
    damageResolvers.add(new FallingBlockDamageResolver());
    damageResolvers.add(new GenericDamageResolver());
  }

  @Override
  public void load() {
    match.addListener(fallTracker, MatchScope.RUNNING);
    match.addListener(fireTracker, MatchScope.RUNNING);
    match.addListener(entityTracker, MatchScope.RUNNING);
    match.addListener(blockTracker, MatchScope.RUNNING);
    match.addListener(new DispenserTracker(this), MatchScope.RUNNING);
    match.addListener(new TNTTracker(this), MatchScope.RUNNING);
    match.addListener(new SpleefTracker(this), MatchScope.RUNNING);
    match.addListener(new OwnedMobTracker(this), MatchScope.RUNNING);
    match.addListener(new ProjectileTracker(this), MatchScope.RUNNING);
    match.addListener(new AnvilTracker(this), MatchScope.RUNNING);
    match.addListener(new CombatLogTracker(this), MatchScope.RUNNING);
    match.addListener(new DeathTracker(this), MatchScope.RUNNING);
  }

  public EntityTracker getEntityTracker() {
    return entityTracker;
  }

  public BlockTracker getBlockTracker() {
    return blockTracker;
  }

  public DamageInfo resolveDamage(EntityDamageEvent damageEvent) {
    if (damageEvent instanceof EntityDamageByEntityEvent) {
      return resolveDamage((EntityDamageByEntityEvent) damageEvent);
    } else if (damageEvent instanceof EntityDamageByBlockEvent) {
      return resolveDamage((EntityDamageByBlockEvent) damageEvent);
    } else {
      return resolveDamage(damageEvent.getCause(), damageEvent.getEntity());
    }
  }

  public DamageInfo resolveDamage(EntityDamageByEntityEvent damageEvent) {
    return resolveDamage(damageEvent.getCause(), damageEvent.getEntity(), damageEvent.getDamager());
  }

  public DamageInfo resolveDamage(EntityDamageByBlockEvent damageEvent) {
    return resolveDamage(damageEvent.getCause(), damageEvent.getEntity(), damageEvent.getDamager());
  }

  public DamageInfo resolveDamage(EntityDamageEvent.DamageCause damageType, Entity victim) {
    return resolveDamage(damageType, victim, (PhysicalInfo) null);
  }

  public DamageInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable Block damager) {
    if (damager == null) return resolveDamage(damageType, victim);
    return resolveDamage(damageType, victim, blockTracker.resolveBlock(damager));
  }

  public DamageInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable Entity damager) {
    if (damager == null) return resolveDamage(damageType, victim);
    return resolveDamage(damageType, victim, entityTracker.resolveEntity(damager));
  }

  private DamageInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable PhysicalInfo damager) {
    // Filter out observers immediately
    if (getMatch().getParticipant(victim) == null) return new NullDamageInfo();

    for (DamageResolver resolver : damageResolvers) {
      DamageInfo resolvedInfo = resolver.resolveDamage(damageType, victim, damager);
      if (resolvedInfo != null) {
        return resolvedInfo;
      }
    }

    // This should never happen
    return new NullDamageInfo();
  }

  public @Nullable PhysicalInfo resolveShooter(ProjectileSource source) {
    if (source instanceof Entity) {
      return entityTracker.resolveEntity((Entity) source);
    } else if (source instanceof BlockProjectileSource) {
      return blockTracker.resolveBlock(((BlockProjectileSource) source).getBlock());
    }
    return null;
  }

  public TrackerInfo resolveInfo(Entity entity) {
    return entityTracker.resolveInfo(entity);
  }

  public @Nullable <T extends TrackerInfo> T resolveInfo(Entity entity, Class<T> infoType) {
    return entityTracker.resolveInfo(entity, infoType);
  }

  /** Use every available means to determine the owner of the given {@link Entity} */
  public @Nullable ParticipantState getOwner(Entity entity) {
    return entityTracker.getOwner(entity);
  }

  public TrackerInfo resolveInfo(Block block) {
    return blockTracker.resolveInfo(block);
  }

  /** Use every available means to determine the owner of the given {@link Block} */
  public @Nullable ParticipantState getOwner(Block block) {
    return blockTracker.getOwner(block);
  }
}
