package tc.oc.pgm.stats;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.util.text.PlayerComponent.player;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.destroyable.DestroyableHealthChange;
import tc.oc.pgm.destroyable.DestroyableHealthChangeEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.flag.event.FlagPickupEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.flag.state.Carried;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.ProjectileInfo;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.menu.InventoryMenuItem;
import tc.oc.pgm.util.menu.pattern.DoubleRowMenuArranger;
import tc.oc.pgm.util.menu.pattern.SingleRowMenuArranger;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

@ListenerScope(MatchScope.LOADED)
public class StatsMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Map<UUID, PlayerStats> allPlayerStats = new HashMap<>();
  // Since Bukkit#getOfflinePlayer reads the cached user files, and those files have an expire date
  // + will be wiped if X amount of players join, we need a separate cache for players with stats
  private final Map<UUID, String> cachedUsernames = new HashMap<>();

  private final boolean verboseStats = PGM.get().getConfiguration().showVerboseStats();
  private final Duration showAfter = PGM.get().getConfiguration().showStatsAfter();
  private final boolean bestStats = PGM.get().getConfiguration().showBestStats();
  private final boolean ownStats = PGM.get().getConfiguration().showOwnStats();
  private final Component verboseStatsTitle = translatable("match.stats.title");

  /** Common formats used by stats with decimals */
  public static final DecimalFormat TWO_DECIMALS = new DecimalFormat("#.##");

  public static final DecimalFormat ONE_DECIMAL = new DecimalFormat("#.#");

  public static final Component HEART_SYMBOL = text("\u2764"); // ❤

  public StatsMatchModule(Match match) {
    this.match = match;
  }

  @EventHandler
  public void onDamage(EntityDamageByEntityEvent event) {
    ParticipantState damager =
        match.needModule(TrackerMatchModule.class).getOwner(event.getDamager());
    ParticipantState damaged = match.getParticipantState(event.getEntity());

    // Prevent tracking damage to entities or self
    if (damaged == null || (damager != null && damaged.getId() == damager.getId())) return;

    boolean bow = event.getDamager() instanceof Arrow;
    // Absorbed damage gets removed so we add it back
    double absorptionHearts = -event.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION);
    double realFinalDamage =
        Math.min(event.getFinalDamage(), ((Player) event.getEntity()).getHealth())
            + absorptionHearts;

    if (damager != null) getPlayerStat(damager).onDamage(realFinalDamage, bow);
    getPlayerStat(damaged).onDamaged(realFinalDamage);
  }

  @EventHandler
  public void onShoot(EntityShootBowEvent event) {
    if (event.getEntity() instanceof Player) {
      MatchPlayer player = match.getPlayer(event.getEntity());
      if (player != null) getPlayerStat(player).onBowShoot();
    }
  }

  @EventHandler
  public void onDestroyableBreak(DestroyableHealthChangeEvent event) {
    DestroyableHealthChange change = event.getChange();
    if (change != null && change.getHealthChange() < 0 && change.getPlayerCause() != null)
      // Health change will be a negative number, so we flip it here to positive for storage
      getPlayerStat(change.getPlayerCause()).onDestroyablePieceBroken(-change.getHealthChange());
  }

  @EventHandler
  public void onFlagCapture(FlagCaptureEvent event) {
    getPlayerStat(event.getCarrier()).onFlagCapture();
  }

  @EventHandler
  public void onFlagHold(FlagPickupEvent event) {
    getPlayerStat(event.getCarrier()).onFlagPickup();
  }

  @EventHandler
  public void onFlagDrop(FlagStateChangeEvent event) {
    if (event.getOldState() instanceof Carried)
      getPlayerStat(((Carried) event.getOldState()).getCarrier()).onFlagDrop();
  }

  @EventHandler
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
        murdererStats.setLongestBowKill(
            victim
                .getState()
                .getLocation()
                .distance(((ProjectileInfo) event.getDamageInfo()).getOrigin()));
      }

      murdererStats.onMurder();

      sendPlayerStats(murderer, murdererStats);
    }
  }

  private void sendPlayerStats(MatchPlayer player, PlayerStats stats) {
    if (player.getSettings().getValue(SettingKey.STATS) == SettingValue.STATS_OFF) return;
    if (stats.getHotbarTask() != null && !stats.getHotbarTask().isDone()) {
      stats.getHotbarTask().cancel(true);
    }
    stats.putHotbarTaskCache(sendLongHotbarMessage(player, stats.getBasicStatsMessage()));
  }

  private Future<?> sendLongHotbarMessage(MatchPlayer player, Component message) {
    Future<?> task =
        match
            .getExecutor(MatchScope.LOADED)
            .scheduleWithFixedDelay(() -> player.sendActionBar(message), 0, 1, TimeUnit.SECONDS);

    match.getExecutor(MatchScope.LOADED).schedule(() -> task.cancel(true), 4, TimeUnit.SECONDS);

    return task;
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {
    if (allPlayerStats.isEmpty() || showAfter.isNegative()) return;

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
    Map<UUID, Integer> allKills = new HashMap<>();
    Map<UUID, Integer> allStreaks = new HashMap<>();
    Map<UUID, Integer> allDeaths = new HashMap<>();
    Map<UUID, Integer> allBowshots = new HashMap<>();
    Map<UUID, Double> allDamage = new HashMap<>();

    for (Map.Entry<UUID, PlayerStats> mapEntry : allPlayerStats.entrySet()) {
      UUID playerUUID = mapEntry.getKey();
      PlayerStats playerStats = mapEntry.getValue();

      getPlayerStat(playerUUID);

      allKills.put(playerUUID, playerStats.getKills());
      allStreaks.put(playerUUID, playerStats.getMaxKillstreak());
      allDeaths.put(playerUUID, playerStats.getDeaths());
      allBowshots.put(playerUUID, playerStats.getLongestBowKill());
      allDamage.put(playerUUID, playerStats.getDamageDone());
    }

    List<Component> best = new ArrayList<>();
    if (event.isShowBest()) {
      best.add(getMessage("match.stats.kills", sortStats(allKills), NamedTextColor.GREEN));
      best.add(getMessage("match.stats.killstreak", sortStats(allStreaks), NamedTextColor.GREEN));
      best.add(getMessage("match.stats.deaths", sortStats(allDeaths), NamedTextColor.RED));

      Map.Entry<UUID, Integer> bestBowshot = sortStats(allBowshots);
      if (bestBowshot.getValue() > 1)
        best.add(getMessage("match.stats.bowshot", bestBowshot, NamedTextColor.YELLOW));

      if (verboseStats) {
        Map.Entry<UUID, Double> bestDamage = sortStatsDouble(allDamage);
        best.add(
            translatable(
                "match.stats.damage",
                playerName(bestDamage.getKey()),
                damageComponent(bestDamage.getValue(), NamedTextColor.GREEN)));
      }
    }

    for (MatchPlayer viewer : match.getPlayers()) {
      if (viewer.getSettings().getValue(SettingKey.STATS) == SettingValue.STATS_OFF) continue;

      viewer.sendMessage(
          TextFormatter.horizontalLineHeading(
              viewer.getBukkit(),
              translatable("match.stats.title", NamedTextColor.YELLOW),
              NamedTextColor.WHITE));

      best.forEach(viewer::sendMessage);

      PlayerStats stats = allPlayerStats.get(viewer.getId());
      if (event.isShowOwn() && stats != null) {
        Component ksHover =
            translatable(
                "match.stats.killstreak.concise",
                numberComponent(stats.getKillstreak(), NamedTextColor.GREEN));

        viewer.sendMessage(
            translatable(
                "match.stats.own",
                numberComponent(stats.getKills(), NamedTextColor.GREEN),
                numberComponent(stats.getMaxKillstreak(), NamedTextColor.GREEN)
                    .hoverEvent(showText(ksHover)),
                numberComponent(stats.getDeaths(), NamedTextColor.RED),
                numberComponent(stats.getKD(), NamedTextColor.GREEN),
                damageComponent(stats.getDamageDone(), NamedTextColor.GREEN)));
      }

      giveVerboseStatsItem(viewer, false);
    }
  }

  @EventHandler
  public void onToolClick(PlayerInteractEvent event) {
    if (!verboseStats
        || !match.isFinished()
        || !match.getCompetitors().stream().allMatch(c -> c instanceof Team)) return;
    Action action = event.getAction();
    if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
      ItemStack item = event.getPlayer().getItemInHand();

      if (item.getType() == Material.PAPER) {
        MatchPlayer player = match.getPlayer(event.getPlayer());
        if (player == null) return;
        giveVerboseStatsItem(player, true);
      }
    }
  }

  public void giveVerboseStatsItem(MatchPlayer player, boolean forceOpen) {
    // Find out if verbose stats is relevant for this match
    final Collection<Competitor> competitors = match.getCompetitors();
    boolean showAllVerboseStats =
        verboseStats && competitors.stream().allMatch(c -> c instanceof Team);
    if (!showAllVerboseStats) return;

    final List<InventoryMenuItem> items =
        competitors.stream()
            .map(c -> new TeamStatsInventoryMenuItem(match, c))
            .collect(Collectors.toList());

    // Add the player item in the middle
    items.add((items.size() - 1) / 2 + 1, new PlayerStatsInventoryMenuItem(player));

    final InventoryMenu menu =
        new InventoryMenu(
            match.getWorld(),
            verboseStatsTitle,
            items,
            competitors.size() <= 4 ? new SingleRowMenuArranger() : new DoubleRowMenuArranger());

    player
        .getInventory()
        .setItem(7, new VerboseStatsInventoryMenuItem(menu).createItem(player.getBukkit()));
    if (forceOpen) menu.display(player.getBukkit());
  }

  private Map.Entry<UUID, Integer> sortStats(Map<UUID, Integer> map) {
    return map.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).orElse(null);
  }

  private Map.Entry<UUID, Double> sortStatsDouble(Map<UUID, Double> map) {
    return map.entrySet().stream()
        .max(Comparator.comparingDouble(Map.Entry::getValue))
        .orElse(null);
  }

  Component getMessage(
      String messageKey, Map.Entry<UUID, ? extends Number> mapEntry, TextColor color) {
    return translatable(
        messageKey, playerName(mapEntry.getKey()), numberComponent(mapEntry.getValue(), color));
  }

  /**
   * Wraps a {@link Number} in a {@link Component} that is colored with the given {@link TextColor}.
   * Rounds the number to a maximum of 2 decimals
   *
   * <p>If the number is NaN "-" is wrapped instead
   *
   * <p>If the number is >= 10000 it will be represented in the thousands (10k, 25.5k, 120.3k etc.)
   *
   * @param stat The number you want wrapped
   * @param color The color you want the number to be
   * @return a colored component wrapping the given number or "-" if NaN
   */
  public static Component numberComponent(Number stat, TextColor color) {
    double doubleStat = stat.doubleValue();
    boolean tenThousand = doubleStat >= 10000;
    String returnValue = null;
    if (Double.isNaN(doubleStat)) returnValue = "-"; // If NaN, dont try to display as a number
    else if (doubleStat % 1 == 0) { // Can the given number can be displayed as an integer?
      int value = stat.intValue();
      if (!tenThousand
          || value % 1000
              == 0) // If the number is above 999 we also need to check if the shortened number can
        // be displayed as an integer
        returnValue = Integer.toString(tenThousand ? value / 1000 : value);
    }
    if (returnValue
        == null) { // If not yet defined, display as a double with either 1 or 2 decimals
      if (tenThousand) doubleStat /= 1000;
      String decimals = Double.toString(doubleStat).split("\\.")[1];
      if (decimals.chars().sum() == 1 || tenThousand) returnValue = ONE_DECIMAL.format(doubleStat);
      else returnValue = TWO_DECIMALS.format(doubleStat);
    }
    return text(returnValue + (tenThousand ? "k" : ""), color);
  }

  /** Formats raw damage to damage relative to the amount of hearths the player would have broken */
  public static Component damageComponent(double damage, TextColor color) {

    double hearts = damage / (double) 2;

    return numberComponent(hearts, color).append(HEART_SYMBOL);
  }

  @EventHandler
  public void onPlayerLeave(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (allPlayerStats.containsKey(player.getUniqueId()))
      cachedUsernames.put(player.getUniqueId(), player.getName());
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    UUID playerUUID = event.getPlayer().getUniqueId();
    cachedUsernames.remove(playerUUID);
  }

  private Component playerName(UUID playerUUID) {
    return player(
        Bukkit.getPlayer(playerUUID),
        cachedUsernames.getOrDefault(playerUUID, "Unknown"),
        NameStyle.FANCY);
  }

  // Creates a new PlayerStat if the player does not have one yet
  public final PlayerStats getPlayerStat(UUID uuid) {
    if (hasNoStats(uuid)) putNewPlayer(uuid);
    return allPlayerStats.get(uuid);
  }

  private void putNewPlayer(UUID player) {
    allPlayerStats.put(player, new PlayerStats());
  }

  public boolean hasNoStats(UUID player) {
    return allPlayerStats.get(player) == null;
  }

  public final PlayerStats getPlayerStat(ParticipantState player) {
    return getPlayerStat(player.getId());
  }

  public final PlayerStats getPlayerStat(MatchPlayer player) {
    return getPlayerStat(player.getId());
  }

  public Component getBasicStatsMessage(UUID player) {
    return getPlayerStat(player).getBasicStatsMessage();
  }
}
