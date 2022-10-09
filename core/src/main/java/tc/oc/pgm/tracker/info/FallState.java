package tc.oc.pgm.tracker.info;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.FallInfo;
import tc.oc.pgm.api.tracker.info.OwnerInfo;
import tc.oc.pgm.api.tracker.info.TrackerInfo;

public class FallState implements FallInfo {

  // A player must leave the ground within this many ticks of being attacked for
  // the fall to be caused by knockback from that attack
  public static final long MAX_KNOCKBACK_TICKS = 20;

  // A player's fall is cancelled if they are on the ground continuously for more than this many
  // ticks
  public static final long MAX_ON_GROUND_TICKS = 10;

  // A player's fall is cancelled if they touch the ground more than this many times
  public static final long MAX_GROUND_TOUCHES = 2;

  // A player's fall is cancelled if they are in water for more than this many ticks
  public static final long MAX_SWIMMING_TICKS = 20;

  // A player's fall is cancelled if they are climbing something for more than this many ticks
  public static final long MAX_CLIMBING_TICKS = 10;

  // How long players burn for after touching lava. This is a vanilla value.
  public static final long MAX_BURNING_TICKS = 255;

  public final MatchPlayer victim;
  public final Location origin;

  // The kind of attack that initiated the fall
  public final From from;
  public final TrackerInfo cause;

  public final Tick startTime;

  // Where they land.. this is set when the fall ends
  public To to;

  // If the player is on the ground when attacked, this is initially set false and later set true
  // when they leave
  // the ground within the allowed time window. If the player is already in the air when attacked,
  // this is set true.
  // This is used to distinguish the initial knockback/spleef from ground touches that occur during
  // the fall.
  public boolean isStarted;

  // Set true when the fall is over and no further processing should be done
  public boolean isEnded;

  // Time the player last transitioned from off-ground to on-ground
  public long onGroundTick;

  // The player's most recent swimming state and the time it was last set true
  public boolean isSwimming;
  public long swimmingTick;

  // The player's most recent climbing state and the time it was last set true
  public boolean isClimbing;
  public long climbingTick;

  // The player's most recent in-lava state and the time it was last set true and false
  public boolean isInLava;
  public long inLavaTick, outLavaTick;

  // The number of times the player has touched the ground during since isFalling was set true
  public int groundTouchCount;

  public FallState(MatchPlayer victim, From from, TrackerInfo cause) {
    this.victim = victim;
    this.from = from;
    this.cause = cause;
    this.startTime = victim.getMatch().getTick();
    this.origin = victim.getBukkit().getLocation();
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    if (cause instanceof OwnerInfo) {
      return ((OwnerInfo) cause).getOwner();
    } else if (cause instanceof DamageInfo) {
      return ((DamageInfo) cause).getAttacker();
    } else {
      return null;
    }
  }

  @Override
  public Location getOrigin() {
    return origin;
  }

  @Override
  public From getFrom() {
    return from;
  }

  @Override
  public To getTo() {
    return to;
  }

  @Override
  public TrackerInfo getCause() {
    return cause;
  }

  /**
   * Check if the victim of this fall is current supported by any solid blocks, water, or ladders
   */
  public boolean isSupported() {
    return this.isClimbing || this.isSwimming || victim.getBukkit().isOnGround();
  }

  /** Check if the victim has failed to become unsupported quickly enough after the fall began */
  public boolean isExpired(Tick now) {
    return this.isSupported() && now.tick - startTime.tick > MAX_KNOCKBACK_TICKS;
  }

  /**
   * Check if this fall has ended safely, which is true if the victim is not in lava and any of the
   * following are true:
   *
   * <p>- victim has been on the ground for MAX_ON_GROUND_TICKS - victim has touched the ground
   * MAX_GROUND_TOUCHES times - victim has been in water for MAX_SWIMMING_TICKS - victim has been on
   * a ladder for MAX_CLIMBING_TICKS
   */
  public boolean isEndedSafely(Tick now) {
    return (!isInLava && now.tick - outLavaTick > MAX_BURNING_TICKS)
        && ((victim.getBukkit().isOnGround()
                && (now.tick - onGroundTick > MAX_ON_GROUND_TICKS
                    || groundTouchCount > MAX_GROUND_TOUCHES))
            || (isSwimming && now.tick - swimmingTick > MAX_SWIMMING_TICKS)
            || (isClimbing && now.tick - climbingTick > MAX_CLIMBING_TICKS));
  }

  /**
   * A new fall can't be initiated if the victim is already falling, unless:
   * <li>The fall never started (i.e. they were never knocked into the air)
   * <li>The player touched the ground at least once since the previous hit (they landed)
   * <li>The player is not burning from this fall
   *
   *     <p>This function indicates if the fall is still ongoing and may not be replaced
   */
  public boolean isOngoing(Tick now) {
    return (isStarted && groundTouchCount == 0)
        || (isInLava || now.tick - outLavaTick <= MAX_BURNING_TICKS);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "victim="
        + victim
        + " origin="
        + origin
        + " from="
        + from
        + " cause="
        + cause
        + " startTime="
        + startTime
        + " to="
        + to
        + " isStarted="
        + isStarted
        + " isEnded="
        + isEnded
        + " onGroundTick="
        + onGroundTick
        + " isSwimming="
        + isSwimming
        + " swimmingTick="
        + swimmingTick
        + " isClimbing="
        + isClimbing
        + " climbingTick="
        + climbingTick
        + " isInLava="
        + isInLava
        + " inLavaTick="
        + inLavaTick
        + " groundTouchCount="
        + groundTouchCount
        + '}';
  }
}
