package tc.oc.pgm.stats;

import static tc.oc.pgm.stats.StatType.ASSISTS;
import static tc.oc.pgm.stats.StatType.DEATHS;
import static tc.oc.pgm.stats.StatType.KILLS;
import static tc.oc.pgm.stats.StatType.KILL_DEATH_RATIO;
import static tc.oc.pgm.stats.StatType.KILL_STREAK;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.Audience;

/** A wrapper for stat info belonging to a {@link tc.oc.pgm.api.player.MatchPlayer} */
public class PlayerStats implements StatHolder {

  // A reference to the players global stats to be incremented along with team based stats
  private final PlayerStats parent;
  private final Component component;

  private Duration timePlayed;
  private Instant inTime;

  // K/D
  private int kills;
  private int deaths;
  private int assists;
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
  private int monumentsDestroyed;

  private int flagsCaptured;
  private int flagPickups;

  private int coresLeaked;

  private int woolsCaptured;
  private int woolsTouched;

  private Duration longestFlagHold = Duration.ZERO;
  private Instant longestFlagHoldCache;

  // The task responsible for displaying the stats over the hotbar
  // See StatsMatchModule#sendLongHotbarMessage
  private Future<?> hotbarTask;

  public PlayerStats() {
    this(null, null);
  }

  public PlayerStats(PlayerStats parent, Component component) {
    this.parent = parent;
    this.component = component;
    this.timePlayed = Duration.ZERO;
    this.inTime = null;
  }

  // Methods to update the stats, should only be accessed by StatsMatchModule

  protected void onMurder(MatchPlayer player) {
    kills++;
    killstreak++;
    if (killstreak > killstreakMax) killstreakMax = killstreak;
    if (parent != null) parent.onMurder(null);
    sendPlayerStats(player);
  }

  protected void onDeath(MatchPlayer player) {
    deaths++;
    killstreak = 0;
    if (parent != null) parent.onDeath(null);
    sendPlayerStats(player);
  }

  protected void onAssist(MatchPlayer player) {
    assists++;
    if (parent != null) parent.onAssist(null);
    sendPlayerStats(player);
  }

  protected void onDamage(double damage, boolean bow) {
    damageDone += damage;
    if (bow) {
      bowDamage += damage;
      shotsHit++;
    }
    if (parent != null) parent.onDamage(damage, bow);
  }

  protected void onDamaged(double damage, boolean bow) {
    damageTaken += damage;
    if (bow) {
      bowDamageTaken += damage;
    }
    if (parent != null) parent.onDamaged(damage, bow);
  }

  protected void onDestroyablePieceBroken(int change) {
    destroyablePiecesBroken += change;
    if (parent != null) parent.onDestroyablePieceBroken(change);
  }

  protected void onMonumentDestroyed() {
    monumentsDestroyed++;
    if (parent != null) parent.onMonumentDestroyed();
  }

  protected void onFlagCapture() {
    flagsCaptured++;
    onFlagDrop();
    if (parent != null) parent.onFlagCapture();
  }

  protected void onFlagPickup(boolean isFirstPickup) {
    if (isFirstPickup) flagPickups++;
    longestFlagHoldCache = Instant.now();
    if (parent != null) parent.onFlagPickup(isFirstPickup);
  }

  protected void onFlagDrop() {
    setLongestFlagHold(
        Duration.ofMillis(Instant.now().toEpochMilli() - longestFlagHoldCache.toEpochMilli()));
    if (parent != null) parent.onFlagDrop();
  }

  protected void setLongestFlagHold(Duration time) {
    if (longestFlagHold == null || (time.toNanos() - longestFlagHold.toNanos()) > 0)
      longestFlagHold = time;
    if (parent != null) parent.setLongestFlagHold(time);
  }

  protected void onCoreLeak() {
    coresLeaked++;
    if (parent != null) parent.onCoreLeak();
  }

  protected void onWoolCapture() {
    woolsCaptured++;
    if (parent != null) parent.onWoolCapture();
  }

  protected void onWoolTouch() {
    woolsTouched++;
    if (parent != null) parent.onWoolTouch();
  }

  protected void setLongestBowKill(double distance) {
    if (distance > longestBowKill) longestBowKill = (int) Math.round(distance);
    if (parent != null) parent.setLongestBowKill(distance);
  }

  protected void onBowShoot() {
    shotsTaken++;
    if (parent != null) parent.onBowShoot();
  }

  protected void onTeamSwitch() {
    killstreak = 0;
    if (parent != null) parent.onTeamSwitch();
  }

  // Makes a simple stat message for this player that fits in one line

  public Component getBasicStatsMessage() {
    return pipeSeparated(KILLS, DEATHS, ASSISTS, KILL_STREAK, KILL_DEATH_RATIO)
        .color(NamedTextColor.GRAY);
  }

  // Getters, both raw stats and some handy calculations
  @Override
  public Number getStat(StatType type) {
    return switch (type) {
      case KILLS -> kills;
      case DEATHS -> deaths;
      case ASSISTS -> assists;
      case KILL_STREAK -> killstreak;
      case BEST_KILL_STREAK -> killstreakMax;
      case KILL_DEATH_RATIO -> getKD();
      case LONGEST_BOW_SHOT -> longestBowKill;
      case DAMAGE -> damageDone;
    };
  }

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

  public int getAssists() {
    return assists;
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

  public int getMonumentsDestroyed() {
    return monumentsDestroyed;
  }

  public int getFlagsCaptured() {
    return flagsCaptured;
  }

  public int getFlagPickups() {
    return flagPickups;
  }

  public int getCoresLeaked() {
    return coresLeaked;
  }

  public int getWoolsCaptured() {
    return woolsCaptured;
  }

  public int getWoolsTouched() {
    return woolsTouched;
  }

  public Duration getLongestFlagHold() {
    return longestFlagHold;
  }

  private void setHotbarTask(Future<?> task) {
    if (hotbarTask != null && !hotbarTask.isDone()) hotbarTask.cancel(true);
    hotbarTask = task;
  }

  public void sendPlayerStats(MatchPlayer player) {
    if (player == null) return;
    if (player.getSettings().getValue(SettingKey.STATS) == SettingValue.STATS_OFF) return;
    setHotbarTask(player
        .getMatch()
        .getExecutor(MatchScope.LOADED)
        .scheduleWithFixedDelay(new HotbarStatsRunner(player), 0, 1, TimeUnit.SECONDS));
  }

  public Component getPlayerComponent() {
    return component;
  }

  public void startParticipation() {
    if (inTime == null) this.inTime = Instant.now();

    if (parent != null) parent.startParticipation();
  }

  public void endParticipation() {
    if (this.inTime == null) return;

    this.timePlayed = timePlayed.plus(getActiveSessionDuration());
    this.inTime = null;

    if (parent != null) parent.endParticipation();
  }

  public Duration getTimePlayed() {
    // If not ended yet add the current session time up
    return inTime == null ? timePlayed : timePlayed.plus(getActiveSessionDuration());
  }

  public Duration getActiveSessionDuration() {
    return inTime == null ? Duration.ZERO : Duration.between(inTime, Instant.now());
  }

  protected class HotbarStatsRunner implements Runnable {
    private final Audience audience;
    private final Component message;
    private int remaining = 4;

    private HotbarStatsRunner(Audience audience) {
      this.audience = audience;
      this.message = getBasicStatsMessage();
    }

    @Override
    public void run() {
      audience.sendActionBar(message);
      if (remaining-- < 0) setHotbarTask(null);
    }
  }
}
