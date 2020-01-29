package tc.oc.pgm.tracker.trackers;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerOnGroundEvent;
import tc.oc.material.Materials;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.damage.DamageInfo;
import tc.oc.pgm.tracker.damage.FallInfo;
import tc.oc.pgm.tracker.damage.FallState;
import tc.oc.pgm.tracker.damage.GenericFallInfo;
import tc.oc.pgm.tracker.damage.PhysicalInfo;
import tc.oc.pgm.tracker.event.PlayerSpleefEvent;
import tc.oc.pgm.tracker.resolvers.DamageResolver;
import tc.oc.util.ClassLogger;

/** Tracks the state of falls caused by other players and resolves the damage caused by them. */
public class FallTracker implements Listener, DamageResolver {
  private final Map<MatchPlayer, FallState> falls = new HashMap<>();

  private final TrackerMatchModule tracker;
  private final Match match;
  private final Logger logger;

  public FallTracker(TrackerMatchModule tracker, Match match) {
    this.tracker = tracker;
    this.match = match;
    this.logger = ClassLogger.get(match.getLogger(), getClass());
  }

  @Override
  public @Nullable FallInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable PhysicalInfo damager) {
    FallState fall = getFall(victim);

    if (fall != null) {
      switch (damageType) {
        case VOID:
          fall.to = FallInfo.To.VOID;
          break;
        case FALL:
          fall.to = FallInfo.To.GROUND;
          break;
        case LAVA:
          fall.to = FallInfo.To.LAVA;
          break;

        case FIRE_TICK:
          if (fall.isInLava) {
            fall.to = FallInfo.To.LAVA;
          } else {
            return null;
          }
          break;

        default:
          return null;
      }

      return fall;
    } else {
      switch (damageType) {
        case FALL:
          return new GenericFallInfo(
              FallInfo.To.GROUND, victim.getLocation(), victim.getFallDistance());
        case VOID:
          return new GenericFallInfo(
              FallInfo.To.VOID, victim.getLocation(), victim.getFallDistance());
      }

      return null;
    }
  }

  @Nullable
  FallState getFall(Entity victim) {
    MatchPlayer player = match.getPlayer(victim);
    if (player == null) return null;

    FallState fall = falls.get(player);
    if (fall == null || !fall.isStarted || fall.isEnded) return null;

    return fall;
  }

  void endFall(FallState fall) {
    endFall(fall.victim);
  }

  void endFall(MatchPlayer victim) {
    FallState fall = this.falls.remove(victim);
    if (fall != null) {
      fall.isEnded = true;
      logger.fine("Ended " + fall);
    }
  }

  void checkFallTimeout(final FallState fall) {
    Tick now = match.getTick();
    if ((fall.isStarted && fall.isEndedSafely(now)) || (!fall.isStarted && fall.isExpired(now))) {

      endFall(fall);
    }
  }

  void scheduleCheckFallTimeout(final FallState fall, final long delay) {
    match
        .getScheduler(MatchScope.RUNNING)
        .runTaskLater(
            delay + 1,
            new Runnable() {
              public void run() {
                if (!fall.isEnded) {
                  checkFallTimeout(fall);
                }
              }
            });
  }

  /**
   * Called whenever the player becomes "unsupported" to check if they were attacked recently enough
   * for the attack to be responsible for the fall
   */
  private void playerBecameUnsupported(FallState fall) {
    if (!fall.isStarted
        && !fall.isSupported()
        && match.getTick().tick - fall.startTime.tick <= FallState.MAX_KNOCKBACK_TICKS) {
      fall.isStarted = true;
      logger.fine("Started " + fall);
    }
  }

  /**
   * Called when a player is damaged in a way that could initiate a Fall, i.e. damage from another
   * entity that causes knockback
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onAttack(final EntityDamageEvent event) {
    // Filter out damage types that don't cause knockback
    switch (event.getCause()) {
      case ENTITY_ATTACK:
      case PROJECTILE:
      case BLOCK_EXPLOSION:
      case ENTITY_EXPLOSION:
      case MAGIC:
      case CUSTOM:
        break;

      default:
        return;
    }

    MatchPlayer victim = match.getParticipant(event.getEntity());
    if (victim == null) return;

    if (this.falls.containsKey(victim)) {
      // A new fall can't be initiated if the victim is already falling
      return;
    }

    Location loc = victim.getBukkit().getLocation();
    boolean isInLava = Materials.isLava(loc);
    boolean isClimbing = Materials.isClimbable(loc);
    boolean isSwimming = Materials.isWater(loc);

    DamageInfo cause = tracker.resolveDamage(event);

    // Note the victim's situation when the attack happened
    FallInfo.From from;
    if (isClimbing) {
      from = FallInfo.From.LADDER;
    } else if (isSwimming) {
      from = FallInfo.From.WATER;
    } else {
      from = FallInfo.From.GROUND;
    }

    FallState fall = new FallState(victim, from, cause);
    this.falls.put(victim, fall);

    fall.isClimbing = isClimbing;
    fall.isSwimming = isSwimming;
    fall.isInLava = isInLava;

    // If the victim is already in the air, immediately confirm that they are falling.
    // Otherwise, the fall will be confirmed when they leave the ground, if it happens
    // within the time window.
    fall.isStarted = !fall.isSupported();

    if (!fall.isStarted) {
      this.scheduleCheckFallTimeout(fall, FallState.MAX_KNOCKBACK_TICKS);
    }

    logger.fine("Attacked " + fall);
  }

  /**
   * Called when a player moves in a way that could affect their fall i.e. landing on a ladder or in
   * liquid
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(final PlayerMoveEvent event) {
    MatchPlayer player = match.getParticipant(event.getPlayer());
    if (player == null) return;

    FallState fall = this.falls.get(player);
    if (fall != null) {
      boolean isClimbing = Materials.isClimbable(event.getTo());
      boolean isSwimming = Materials.isWater(event.getTo());
      boolean isInLava = Materials.isLava(event.getTo());
      boolean becameUnsupported = false;
      Tick now = match.getTick();

      if (isClimbing != fall.isClimbing) {
        if ((fall.isClimbing = isClimbing)) {
          // Player moved onto a ladder, cancel the fall if they are still on it after
          // MAX_CLIMBING_TIME
          fall.climbingTick = now.tick;
          this.scheduleCheckFallTimeout(fall, FallState.MAX_CLIMBING_TICKS + 1);
        } else {
          becameUnsupported = true;
        }
      }

      if (isSwimming != fall.isSwimming) {
        if ((fall.isSwimming = isSwimming)) {
          // Player moved into water, cancel the fall if they are still in it after
          // MAX_SWIMMING_TIME
          fall.swimmingTick = now.tick;
          this.scheduleCheckFallTimeout(fall, FallState.MAX_SWIMMING_TICKS + 1);
        } else {
          becameUnsupported = true;
        }
      }

      if (becameUnsupported) {
        // Player moved out of water or off a ladder, check if it was caused by the attack
        this.playerBecameUnsupported(fall);
      }

      if (isInLava != fall.isInLava) {
        if ((fall.isInLava = isInLava)) {
          fall.inLavaTick = now.tick;
        } else {
          // Because players continue to "fall" as long as they are in lava, moving out of lava
          // can immediately finish their fall
          this.checkFallTimeout(fall);
        }
      }
    }
  }

  /** Called when the player touches or leaves the ground */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerOnGroundChanged(final PlayerOnGroundEvent event) {
    MatchPlayer player = match.getParticipant(event.getPlayer());
    if (player == null) return;

    FallState fall = this.falls.get(player);
    if (fall != null) {
      if (event.getOnGround()) {
        // Falling player landed on the ground, cancel the fall if they are still there after
        // MAX_ON_GROUND_TIME
        fall.onGroundTick = match.getTick().tick;
        fall.groundTouchCount++;
        this.scheduleCheckFallTimeout(fall, FallState.MAX_ON_GROUND_TICKS + 1);
      } else {
        // Falling player left the ground, check if it was caused by the attack
        this.playerBecameUnsupported(fall);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerSpleef(final PlayerSpleefEvent event) {
    MatchPlayer victim = event.getVictim();
    FallState fall = this.falls.get(victim);
    if (fall == null || !fall.isStarted) {
      if (fall != null) {
        // End the existing fall and replace it with the spleef
        endFall(fall);
      }

      fall = new FallState(victim, FallInfo.From.GROUND, event.getSpleefInfo());
      fall.isStarted = true;

      Location loc = victim.getBukkit().getLocation();
      fall.isClimbing = Materials.isClimbable(loc);
      fall.isSwimming = Materials.isWater(loc);
      fall.isInLava = Materials.isLava(loc);

      this.falls.put(victim, fall);

      logger.fine("Spleefed " + fall);
    }
  }

  // NOTE: This must be called after anything that tries to resolve the death
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(final PlayerDeathEvent event) {
    MatchPlayer player = match.getParticipant(event.getEntity());
    if (player != null) endFall(player);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDespawn(final ParticipantDespawnEvent event) {
    endFall(event.getPlayer());
  }
}
