package tc.oc.pgm.stats;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.NumberComponent.number;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Future;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** A wrapper for stat info belonging to a {@link tc.oc.pgm.api.player.MatchPlayer} */
public class PlayerStats {
  // K/D
  private int kills;
  private int deaths;
  private int killstreak; // Current killstreak
  private int killstreakMax; // The highest killstreak reached this match

  // Bow
  private int longestBowKill;
  private double bowDamage;
  private double bowDamageTaken;
  private int shotsTaken;
  private int shotsHit;

  // Damage
  private double damageDone;
  private double damageTaken;

  // Objectives
  private int destroyablePiecesBroken;
  private int flagsCaptured;

  private Duration longestFlagHold = Duration.ZERO;
  private Instant longestFlagHoldCache;

  // The task responsible for displaying the stats over the hotbar
  // See StatsMatchModule#sendLongHotbarMessage
  private Future<?> hotbarTaskCache;

  // Methods to update the stats, should only be accessed by StatsMatchModule

  protected void onMurder() {
    kills++;
    killstreak++;
    if (killstreak > killstreakMax) killstreakMax = killstreak;
  }

  protected void onDeath() {
    deaths++;
    killstreak = 0;
  }

  protected void onDamage(double damage, boolean bow) {
    damageDone += damage;
    if (bow) {
      bowDamage += damage;
      shotsHit++;
    }
  }

  protected void onDamaged(double damage, boolean bow) {
    damageTaken += damage;
    if (bow) {
      bowDamageTaken += damage;
    }
  }

  protected void onDestroyablePieceBroken(int change) {
    destroyablePiecesBroken += change;
  }

  protected void onFlagCapture() {
    flagsCaptured++;
    onFlagDrop();
  }

  protected void onFlagPickup() {
    longestFlagHoldCache = Instant.now();
  }

  protected void onFlagDrop() {
    setLongestFlagHold(
        Duration.ofMillis(Instant.now().toEpochMilli() - longestFlagHoldCache.toEpochMilli()));
  }

  protected void setLongestFlagHold(Duration time) {
    if (longestFlagHold == null || (time.toNanos() - longestFlagHold.toNanos()) > 0)
      longestFlagHold = time;
  }

  protected void setLongestBowKill(double distance) {
    if (distance > longestBowKill) {
      longestBowKill = (int) Math.round(distance);
    }
  }

  protected void onBowShoot() {
    shotsTaken++;
  }

  // Makes a simple stat message for this player that fits in one line

  public Component getBasicStatsMessage() {
    return translatable(
        "match.stats",
        NamedTextColor.GRAY,
        number(kills, NamedTextColor.GREEN),
        number(killstreak, NamedTextColor.GREEN),
        number(deaths, NamedTextColor.RED),
        number(getKD(), NamedTextColor.GREEN));
  }

  // Getters, both raw stats and some handy calculations

  public double getKD() {
    return kills / Math.max(1d, deaths);
  }

  // 0..100
  public double getArrowAccuracy() {
    if (shotsTaken == 0) return Double.NaN;
    return shotsHit / (shotsTaken / (double) 100);
  }

  public int getKills() {
    return kills;
  }

  public int getDeaths() {
    return deaths;
  }

  public int getKillstreak() {
    return killstreak;
  }

  public int getMaxKillstreak() {
    return killstreakMax;
  }

  public int getLongestBowKill() {
    return longestBowKill;
  }

  public double getBowDamage() {
    return bowDamage;
  }

  public double getBowDamageTaken() {
    return bowDamageTaken;
  }

  public int getShotsTaken() {
    return shotsTaken;
  }

  public int getShotsHit() {
    return shotsHit;
  }

  public double getDamageDone() {
    return damageDone;
  }

  public double getDamageTaken() {
    return damageTaken;
  }

  public int getDestroyablePiecesBroken() {
    return destroyablePiecesBroken;
  }

  public int getFlagsCaptured() {
    return flagsCaptured;
  }

  public Duration getLongestFlagHold() {
    return longestFlagHold;
  }

  public Future<?> getHotbarTask() {
    return hotbarTaskCache;
  }

  public void putHotbarTaskCache(Future<?> task) {
    hotbarTaskCache = task;
  }
}
