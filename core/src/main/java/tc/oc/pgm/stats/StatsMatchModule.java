package tc.oc.pgm.stats;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.NumberComponent.number;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.core.CoreLeakEvent;
import tc.oc.pgm.destroyable.DestroyableDestroyedEvent;
import tc.oc.pgm.destroyable.DestroyableHealthChange;
import tc.oc.pgm.destroyable.DestroyableHealthChangeEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.events.PlayerLeavePartyEvent;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.flag.state.Carried;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.stats.menu.StatsMainMenu;
import tc.oc.pgm.stats.menu.items.PlayerStatsMenuItem;
import tc.oc.pgm.stats.menu.items.TeamStatsMenuItem;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.ProjectileInfo;
import tc.oc.pgm.util.Pair;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.usernames.UsernameResolvers;
import tc.oc.pgm.wool.MonumentWool;
import tc.oc.pgm.wool.PlayerWoolPlaceEvent;

@ListenerScope(MatchScope.LOADED)
public class StatsMatchModule implements MatchModule, Listener {
  private static final Component HEART_SYMBOL = text("\u2764"); // ❤

  private static final String BOW_KEY = "bow";
  private static final MetadataValue TRUE = new FixedMetadataValue(PGM.get(), true);

  private final Match match;
  private final Map<UUID, PlayerStats> allPlayerStats = new HashMap<>();
  private final Table<Team, UUID, PlayerStats> stats = HashBasedTable.create();

  private final boolean verboseStats = PGM.get().getConfiguration().showVerboseStats();
  private final Duration showAfter = PGM.get().getConfiguration().showStatsAfter();
  private final boolean bestStats = PGM.get().getConfiguration().showBestStats();
  private final boolean ownStats = PGM.get().getConfiguration().showOwnStats();
  private final int verboseItemSlot = PGM.get().getConfiguration().getVerboseItemSlot();

  private List<MenuItem> teams;

  public StatsMatchModule(Match match) {
    this.match = match;
  }

  public Map<UUID, PlayerStats> getStats() {
    return Collections.unmodifiableMap(allPlayerStats);
  }

  public Table<Team, UUID, PlayerStats> getParticipationStats() {
    return Tables.unmodifiableTable(stats);
  }

  @EventHandler
  public void onMatchStart(final MatchStartEvent event) {
    event.getMatch().getParticipants().forEach(player -> getPlayerStat(player)
        .startParticipation());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onMatchFinish(final MatchFinishEvent event) {
    event.getMatch().getParticipants().forEach(player -> getPlayerStat(player).endParticipation());
  }

  @EventHandler
  public void onPlayerJoinMatch(final PlayerJoinPartyEvent event) {
    // Only modify trackers when match is running
    if (!event.getMatch().isRunning()) return;

    // End time tracking for old party
    if (event.getOldParty() instanceof Competitor) {
      computeTeamStatsIfAbsent(event.getPlayer().getId(), event.getOldParty()).endParticipation();
    }

    // When joining a party that's playing, start time tracking
    if (event.getNewParty() instanceof Competitor) {
      computeTeamStatsIfAbsent(event.getPlayer().getId(), event.getNewParty()).startParticipation();
    }
  }

  @EventHandler
  public void onPlayerLeaveMatch(final PlayerLeavePartyEvent event) {
    if (event.getMatch().isRunning() && event.getParty() instanceof Competitor) {
      getPlayerStat(event.getPlayer()).endParticipation();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDamage(EntityDamageByEntityEvent event) {
    ParticipantState damager =
        match.needModule(TrackerMatchModule.class).getOwner(event.getDamager());
    ParticipantState damaged = match.getParticipantState(event.getEntity());

    // Prevent tracking damage to entities or self
    if (damaged == null || (damager != null && damaged.getId() == damager.getId())) return;

    boolean bow = event.getDamager().hasMetadata(BOW_KEY);
    // Absorbed damage gets removed so we add it back
    double absHearts = -event.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION);
    double realFinalDamage =
        Math.min(event.getFinalDamage(), ((Player) event.getEntity()).getHealth()) + absHearts;
    if (realFinalDamage <= 0) return;

    if (damager != null) getPlayerStat(damager).onDamage(realFinalDamage, bow);
    getPlayerStat(damaged).onDamaged(realFinalDamage, bow);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onShoot(EntityShootBowEvent event) {
    if (event.getEntity() instanceof Player) {
      MatchPlayer player = match.getPlayer(event.getEntity());
      if (player != null) getPlayerStat(player).onBowShoot();
      event.getProjectile().setMetadata(BOW_KEY, TRUE);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onDestroyableBreak(DestroyableHealthChangeEvent event) {
    DestroyableHealthChange change = event.getChange();
    if (change != null && change.getHealthChange() < 0 && change.getPlayerCause() != null)
      // Health change will be a negative number, so we flip it here to positive for storage
      getPlayerStat(change.getPlayerCause()).onDestroyablePieceBroken(-change.getHealthChange());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMonumentDestroy(DestroyableDestroyedEvent event) {
    event.getDestroyable().getContributions().forEach(destroyer -> {
      if (destroyer.getPlayerState() != null) {
        getPlayerStat(destroyer.getPlayerState()).onMonumentDestroyed();
      }
    });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCoreLeak(CoreLeakEvent event) {
    event.getCore().getContributions().forEach(leaker -> {
      if (leaker.getPlayerState() != null) {
        getPlayerStat(leaker.getPlayerState()).onCoreLeak();
      }
    });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onGoalTouch(GoalTouchEvent event) {
    if (event.getPlayer() == null) return;

    if (event.getGoal() instanceof MonumentWool) {
      if (event.isFirstForPlayer()) {
        getPlayerStat(event.getPlayer()).onWoolTouch();
      }
    }

    if (event.getGoal() instanceof Flag) {
      getPlayerStat(event.getPlayer()).onFlagPickup(event.isFirstForPlayer());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onWoolCapture(PlayerWoolPlaceEvent event) {
    if (event.getPlayer() != null) {
      getPlayerStat(event.getPlayer()).onWoolCapture();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onFlagCapture(FlagCaptureEvent event) {
    getPlayerStat(event.getCarrier()).onFlagCapture();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onFlagDrop(FlagStateChangeEvent event) {
    if (event.getOldState() instanceof Carried)
      getPlayerStat(((Carried) event.getOldState()).getCarrier()).onFlagDrop();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    MatchPlayer victim = event.getVictim();
    MatchPlayer murderer = null;

    if (event.getKiller() != null)
      murderer = event.getKiller().getParty().getPlayer(event.getKiller().getId());

    PlayerStats victimStats = getPlayerStat(victim);

    victimStats.onDeath();

    sendPlayerStats(victim, victimStats);

    if (murderer != null
        && PlayerRelation.get(victim.getParticipantState(), murderer) != PlayerRelation.ALLY
        && PlayerRelation.get(victim.getParticipantState(), murderer) != PlayerRelation.SELF) {

      PlayerStats murdererStats = getPlayerStat(murderer);

      if (event.getDamageInfo() instanceof ProjectileInfo) {
        murdererStats.setLongestBowKill(victim
            .getState()
            .getLocation()
            .distance(((ProjectileInfo) event.getDamageInfo()).getOrigin()));
      }

      murdererStats.onMurder();

      sendPlayerStats(murderer, murdererStats);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onParticipationStop(PlayerParticipationStopEvent event) {
    getPlayerStat(event.getPlayer()).onTeamSwitch();
  }

  private void sendPlayerStats(MatchPlayer player, PlayerStats stats) {
    if (player.getSettings().getValue(SettingKey.STATS) == SettingValue.STATS_OFF) return;
    if (stats.getHotbarTask() != null && !stats.getHotbarTask().isDone()) {
      stats.getHotbarTask().cancel(true);
    }
    stats.putHotbarTaskCache(sendLongHotbarMessage(player, stats.getBasicStatsMessage()));
  }

  private Future<?> sendLongHotbarMessage(MatchPlayer player, Component message) {
    Future<?> task = match
        .getExecutor(MatchScope.LOADED)
        .scheduleWithFixedDelay(() -> player.sendActionBar(message), 0, 1, TimeUnit.SECONDS);

    match.getExecutor(MatchScope.LOADED).schedule(() -> task.cancel(true), 4, TimeUnit.SECONDS);

    return task;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(MatchFinishEvent event) {
    if (allPlayerStats.isEmpty() || showAfter.isNegative()) return;

    // Try to ensure that usernames for all relevant offline players will be loaded in the cache
    // when the inventory GUI is created. If usernames needs to be resolved using the mojang api
    // (UsernameResolver) it can take some time, and we cant really know how long.
    UsernameResolvers.startBatch();
    this.getOfflinePlayersWithStats().forEach(id -> PGM.get().getDatastore().getUsername(id));
    UsernameResolvers.endBatch();

    // Schedule displaying stats after match end
    match
        .getExecutor(MatchScope.LOADED)
        .schedule(
            () -> match.callEvent(new MatchStatsEvent(match, bestStats, ownStats)),
            showAfter.toMillis(),
            TimeUnit.MILLISECONDS);
  }

  @EventHandler(ignoreCancelled = true)
  public void onStatsDisplay(MatchStatsEvent event) {
    if (allPlayerStats.isEmpty()) return;

    // Gather all player stats from this match
    Pair<UUID, Integer> bestKills = null;
    Pair<UUID, Integer> bestStreaks = null;
    Pair<UUID, Integer> bestDeaths = null;
    Pair<UUID, Integer> bestBowShots = null;
    Pair<UUID, Double> bestDamage = null;

    for (Map.Entry<UUID, PlayerStats> mapEntry : allPlayerStats.entrySet()) {
      UUID uuid = mapEntry.getKey();
      PlayerStats s = mapEntry.getValue();
      bestKills = getBest(bestKills, uuid, s.getKills());
      bestStreaks = getBest(bestStreaks, uuid, s.getMaxKillstreak());
      bestDeaths = getBest(bestDeaths, uuid, s.getDeaths());
      bestBowShots = getBest(bestBowShots, uuid, s.getLongestBowKill());
      bestDamage = getBest(bestDamage, uuid, s.getDamageDone());
    }

    List<Component> best = new ArrayList<>();
    if (event.isShowBest()) {
      best.add(getMessage("match.stats.kills", bestKills, NamedTextColor.GREEN));
      best.add(getMessage("match.stats.killstreak", bestStreaks, NamedTextColor.GREEN));
      best.add(getMessage("match.stats.deaths", bestDeaths, NamedTextColor.RED));

      if (bestBowShots.getRight() > 0)
        best.add(getMessage("match.stats.bowshot", bestBowShots, NamedTextColor.YELLOW));

      if (verboseStats) {
        best.add(translatable(
            "match.stats.damage",
            player(bestDamage.getLeft(), NameStyle.VERBOSE),
            damageComponent(bestDamage.getRight(), NamedTextColor.GREEN)));
      }
    }

    for (MatchPlayer viewer : match.getPlayers()) {
      if (viewer.getSettings().getValue(SettingKey.STATS) == SettingValue.STATS_OFF) continue;

      viewer.sendMessage(TextFormatter.horizontalLineHeading(
          viewer.getBukkit(),
          translatable("match.stats.title", NamedTextColor.YELLOW),
          NamedTextColor.WHITE));

      best.forEach(viewer::sendMessage);

      PlayerStats stats = getPlayerStat(viewer);

      if (event.isShowOwn() && stats != null) {
        Component ksHover = translatable(
            "match.stats.killstreak.concise", number(stats.getKillstreak(), NamedTextColor.GREEN));

        viewer.sendMessage(translatable(
            "match.stats.own",
            number(stats.getKills(), NamedTextColor.GREEN),
            number(stats.getMaxKillstreak(), NamedTextColor.GREEN).hoverEvent(showText(ksHover)),
            number(stats.getDeaths(), NamedTextColor.RED),
            number(stats.getKD(), NamedTextColor.GREEN),
            damageComponent(stats.getDamageDone(), NamedTextColor.GREEN)));
      }

      giveVerboseStatsItem(viewer, false);
    }
  }

  @EventHandler
  public void onToolClick(PlayerInteractEvent event) {
    if (event.getPlayer().getItemInHand().getType() != Material.PAPER) return;
    if (!match.isFinished() || !verboseStats) return;
    Action action = event.getAction();
    if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
      MatchPlayer player = match.getPlayer(event.getPlayer());
      if (player != null) giveVerboseStatsItem(player, true);
    }
  }

  public PlayerStatsMenuItem getPlayerStatsItem(MatchPlayer player) {
    return new PlayerStatsMenuItem(
        player.getId(),
        this.getGlobalPlayerStat(player),
        PLAYER_UTILS.getPlayerSkin(player.getBukkit()));
  }

  public void giveVerboseStatsItem(MatchPlayer player, boolean forceOpen) {
    if (!verboseStats) return;
    final Collection<Competitor> competitors = match.getSortedCompetitors();

    if (teams == null) {
      teams = Lists.newArrayList();
      for (Competitor competitor : competitors) {
        if (competitor instanceof Team t) {
          teams.add(new TeamStatsMenuItem(match, competitor, stats.row(t)));
        } else if (competitor instanceof Tribute t) {
          MatchPlayer tribute = t.getPlayer();
          if (tribute != null) teams.add(getPlayerStatsItem(tribute));
        }
      }
    }

    StatsMainMenu menu = new StatsMainMenu(player, teams, this);
    player.getInventory().setItem(verboseItemSlot, menu.getItem());
    if (forceOpen) menu.open();
  }

  private <T extends Comparable<T>> Pair<UUID, T> getBest(Pair<UUID, T> curr, UUID uuid, T alt) {
    return curr != null && curr.getRight().compareTo(alt) >= 0 ? curr : Pair.of(uuid, alt);
  }

  Component getMessage(String messageKey, Pair<UUID, ? extends Number> mapEntry, TextColor color) {
    return translatable(
        messageKey,
        player(mapEntry.getLeft(), NameStyle.VERBOSE),
        number(mapEntry.getRight(), color));
  }

  /** Formats raw damage to damage relative to the amount of hearths the player would have broken */
  public static Component damageComponent(double damage, TextColor color) {
    double hearts = damage / (double) 2;
    return number(hearts, color).append(HEART_SYMBOL);
  }

  private Stream<UUID> getOfflinePlayersWithStats() {
    return allPlayerStats.keySet().stream().filter(id -> match.getPlayer(id) == null);
  }

  @Deprecated
  public final PlayerStats getPlayerStat(UUID uuid) {
    return getGlobalPlayerStat(uuid);
  }

  private void putNewPlayer(UUID player) {
    allPlayerStats.put(player, new PlayerStats());
  }

  private PlayerStats computeTeamStatsIfAbsent(UUID id, Party party) {
    // Only players on a team have team specific stats
    PlayerStats globalStats = getGlobalPlayerStat(id);
    if (!(party instanceof Team team)) return globalStats;

    PlayerStats playerStats = stats.get(team, id);
    if (playerStats != null) return playerStats;

    MatchPlayer player = team.getPlayer(id);
    if (player == null) return globalStats;

    // Create player team stats with reference to global stats
    playerStats = new PlayerStats(globalStats, player.getName());
    stats.put(team, id, playerStats);

    return playerStats;
  }

  public boolean hasNoStats(UUID player) {
    return allPlayerStats.get(player) == null;
  }

  public final PlayerStats getGlobalPlayerStat(MatchPlayer player) {
    return getGlobalPlayerStat(player.getId());
  }

  // Creates a new PlayerStat if the player does not have one yet
  public final PlayerStats getGlobalPlayerStat(UUID uuid) {
    if (hasNoStats(uuid)) putNewPlayer(uuid);
    return allPlayerStats.get(uuid);
  }

  public final PlayerStats getPlayerStat(ParticipantState player) {
    return computeTeamStatsIfAbsent(player.getId(), player.getParty());
  }

  public final PlayerStats getPlayerStat(MatchPlayer player) {
    return computeTeamStatsIfAbsent(player.getId(), player.getParty());
  }

  private PlayerStats getPlayerStat(MatchPlayerState playerState) {
    return computeTeamStatsIfAbsent(playerState.getId(), playerState.getParty());
  }

  public Component getBasicStatsMessage(UUID player) {
    return getGlobalPlayerStat(player).getBasicStatsMessage();
  }

  /**
   * Retrieves the team the player has spent the most time in, may include/exclude observing time.
   *
   * @param uuid The UUID of the player.
   * @param includeObservers Should observers be considered for the primary team
   * @return Primary team of the player, null if no team found or observer time exceeds playtime
   */
  public Team getPrimaryTeam(UUID uuid, boolean includeObservers) {
    Map.Entry<Team, PlayerStats> primaryTeam = stats.column(uuid).entrySet().stream()
        .max(Comparator.comparing(entry -> entry.getValue().getTimePlayed()))
        .orElse(null);

    if (primaryTeam == null) return null;

    if (includeObservers) {
      // If the player has spent more time in observers than teams return null
      Duration obsTime = match.getDuration().minus(getGlobalPlayerStat(uuid).getTimePlayed());
      return (obsTime.compareTo(primaryTeam.getValue().getTimePlayed()) > 0)
          ? null
          : primaryTeam.getKey();
    }

    return primaryTeam.getKey();
  }
}
