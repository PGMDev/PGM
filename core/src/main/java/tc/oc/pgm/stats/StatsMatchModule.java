package tc.oc.pgm.stats;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.NumberComponent.number;
import static tc.oc.pgm.util.text.TextFormatter.list;

import com.google.common.collect.Collections2;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import tc.oc.pgm.stats.menu.items.VerboseStatsMenuItem;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.ProjectileInfo;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;
import tc.oc.pgm.util.text.RenderableComponent;
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
      getPlayerTeamStats(event.getPlayer().getId(), event.getOldParty()).endParticipation();
    }

    // When joining a party that's playing, start time tracking
    if (event.getNewParty() instanceof Competitor) {
      getPlayerTeamStats(event.getPlayer().getId(), event.getNewParty()).startParticipation();
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
    getPlayerStat(victim).onDeath(victim);

    if (event.isChallengeKill()) {
      MatchPlayerState killer = event.getKiller();
      assert killer != null;
      PlayerStats murdererStats = getPlayerStat(killer);
      if (event.getDamageInfo() instanceof ProjectileInfo projectile)
        murdererStats.setLongestBowKill(victim.getLocation().distance(projectile.getOrigin()));
      murdererStats.onMurder(killer.getPlayer().orElse(null));
    }

    if (event.isChallengeAssist()) {
      MatchPlayerState assister = event.getAssister();
      assert assister != null;
      getPlayerStat(assister).onAssist(assister.getPlayer().orElse(null));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onParticipationStop(PlayerParticipationStopEvent event) {
    getPlayerStat(event.getPlayer()).onTeamSwitch();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(MatchFinishEvent event) {
    if (allPlayerStats.isEmpty() || showAfter.isNegative()) return;

    // Try to ensure that usernames for all relevant offline players will be loaded in the cache
    // when the inventory GUI is created. If usernames needs to be resolved using the mojang api
    // (UsernameResolver) it can take some time, and we cant really know how long.
    UsernameResolvers.startBatch();
    allPlayerStats.keySet().stream()
        .filter(id -> match.getPlayer(id) == null)
        .forEach(id -> PGM.get().getDatastore().getUsername(id));
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
    if (allPlayerStats.isEmpty() || (!event.isShowOwn() && !event.isShowBest())) return;

    // Gather aggregated player stats from this match
    List<AggStat<?>> stats = new ArrayList<>();
    stats.add(new AggStat<>(StatType.KILLS, 0, new HashSet<>()));
    stats.add(new AggStat<>(StatType.DEATHS, 0, new HashSet<>()));
    stats.add(new AggStat<>(StatType.ASSISTS, 0, new HashSet<>()));
    stats.add(new AggStat<>(StatType.BEST_KILL_STREAK, 0, new HashSet<>()));
    stats.add(new AggStat<>(StatType.LONGEST_BOW_SHOT, 0, new HashSet<>()));
    if (verboseStats) stats.add(new AggStat<>(StatType.DAMAGE, 0d, new HashSet<>()));

    allPlayerStats.forEach((uuid, s) -> stats.replaceAll(stat -> stat.track(uuid, s)));

    var best = stats.stream()
        .filter(agg -> agg.value().doubleValue() > 0)
        .map(stat -> getMessage(stat, event.isShowBest(), event.isShowOwn()))
        .toList();

    var item = new VerboseStatsMenuItem();
    for (MatchPlayer viewer : match.getPlayers()) {
      if (viewer.getSettings().getValue(SettingKey.STATS) == SettingValue.STATS_OFF) continue;

      viewer.sendMessage(TextFormatter.horizontalLineHeading(
          viewer.getBukkit(),
          translatable("match.stats.title", NamedTextColor.YELLOW),
          NamedTextColor.WHITE));

      best.forEach(viewer::sendMessage);
      viewer.getInventory().setItem(verboseItemSlot, item.createItem(viewer.getBukkit()));
    }
  }

  @EventHandler
  public void onToolClick(PlayerInteractEvent event) {
    if (event.getPlayer().getItemInHand().getType() != Material.PAPER) return;
    if (!match.isFinished() || !verboseStats) return;
    Action action = event.getAction();
    if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
      MatchPlayer player = match.getPlayer(event.getPlayer());
      if (player != null) openStatsMenu(player);
    }
  }

  public void openStatsMenu(MatchPlayer player) {
    if (!verboseStats) return;
    if (teams == null) {
      var competitors = match.getSortedCompetitors();
      teams = new ArrayList<>(competitors.size());
      for (Competitor competitor : competitors) {
        MatchPlayer tribute;
        if (competitor instanceof Team t)
          teams.add(new TeamStatsMenuItem(match, competitor, stats.row(t)));
        else if (competitor instanceof Tribute t && (tribute = t.getPlayer()) != null)
          teams.add(new PlayerStatsMenuItem(tribute, getGlobalPlayerStat(tribute)));
      }
    }
    new StatsMainMenu(player, teams, this).open();
  }

  private Component getMessage(AggStat<?> agg, boolean best, boolean own) {
    Component who = empty();
    if (best)
      who = translatable("misc.authorship", agg.type.makeNumber(agg.value), credit(agg.players));
    if (own)
      who = who.append((RenderableComponent) v -> {
        if (!(v instanceof Player p)) return empty();
        if (agg.players.contains(p.getUniqueId()) || hasNoStats(p.getUniqueId())) return empty();
        var number = agg.type.makeNumber(getGlobalPlayerStat(p.getUniqueId()).getStat(agg.type));
        return !best ? number : text("   ").append(translatable("match.stats.you.short", number));
      });
    return translatable(agg.type.key, who);
  }

  private Component credit(Set<UUID> players) {
    if (players.size() >= 10)
      return translatable("objective.credit.many", NamedTextColor.GRAY, TextDecoration.ITALIC);

    var list = list(Collections2.transform(players, this::getPlayerComponent), null);
    if (players.size() > 3)
      return translatable("match.stats.severalPlayers", NamedTextColor.GRAY, TextDecoration.ITALIC)
          .hoverEvent(list);
    return list;
  }

  private Component getPlayerComponent(UUID uuid) {
    var player = player(uuid, NameStyle.VERBOSE);
    if (player != PlayerComponent.UNKNOWN_PLAYER && player != PlayerComponent.UNKNOWN)
      return player;
    return stats.column(uuid).values().stream()
        .max(Comparator.comparing(PlayerStats::getTimePlayed))
        .map(PlayerStats::getPlayerComponent)
        .orElse(player);
  }

  /** Formats raw damage to damage relative to the amount of hearths the player would have broken */
  public static Component damageComponent(double damage, TextColor color) {
    double hearts = damage / (double) 2;
    return number(hearts, color).append(HEART_SYMBOL);
  }

  @Deprecated
  public final PlayerStats getPlayerStat(UUID uuid) {
    return getGlobalPlayerStat(uuid);
  }

  private PlayerStats getPlayerTeamStats(UUID id, Party party) {
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
    return !allPlayerStats.containsKey(player);
  }

  public final PlayerStats getGlobalPlayerStat(MatchPlayer player) {
    return getGlobalPlayerStat(player.getId());
  }

  // Creates a new PlayerStat if the player does not have one yet
  public final PlayerStats getGlobalPlayerStat(UUID uuid) {
    return allPlayerStats.computeIfAbsent(uuid, k -> new PlayerStats());
  }

  public final PlayerStats getPlayerStat(ParticipantState player) {
    return getPlayerTeamStats(player.getId(), player.getParty());
  }

  public final PlayerStats getPlayerStat(MatchPlayer player) {
    return getPlayerTeamStats(player.getId(), player.getParty());
  }

  private PlayerStats getPlayerStat(MatchPlayerState playerState) {
    return getPlayerTeamStats(playerState.getId(), playerState.getParty());
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

  private record AggStat<T extends Number & Comparable<T>>(
      StatType type, T value, Set<UUID> players) {
    public AggStat<T> track(UUID uuid, StatHolder stat) {
      T newVal = (T) stat.getStat(type);
      int cmp = value.compareTo(newVal);
      if (cmp > 0) return this;
      if (cmp < 0) players.clear();
      players.add(uuid);
      return cmp == 0 ? this : new AggStat<>(type, newVal, players);
    }
  }
}
