package tc.oc.pgm.stats;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuItem;
import tc.oc.pgm.menu.InventoryMenuUtils;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.ProjectileInfo;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.types.PlayerComponent;

@ListenerScope(MatchScope.LOADED)
public class StatsMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Map<UUID, PlayerStats> allPlayerStats = new HashMap<>();
  // Since Bukkit#getOfflinePlayer reads the cached user files, and those files have an expire date
  // + will be wiped if X amount of players join, we need a separate cache for players with stats
  private final Map<UUID, String> cachedUsernames = new HashMap<>();

  private final boolean verboseStats = PGM.get().getConfiguration().showVerboseStats();
  private final Component verboseStatsTitle = TranslatableComponent.of("match.stats.title");

  /** A common format used by all stats with decimals */
  public static final DecimalFormat STATS_DECIMALFORMAT = new DecimalFormat("#.##");

  // Defined at match end, see #onMatchEnd
  private InventoryMenu endOfMatchMenu;

  public StatsMatchModule(Match match) {
    this.match = match;
  }

  @EventHandler
  public void onDamage(EntityDamageByEntityEvent event) {
    ParticipantState damager =
        match.needModule(TrackerMatchModule.class).getOwner(event.getDamager());
    ParticipantState damaged = match.getParticipantState(event.getEntity());
    if ((damaged != null && damager != null) && damaged.getId() == damager.getId()) return;
    boolean bow = event.getDamager() instanceof Arrow;
    if (damager != null) getPlayerStat(damager).onDamage(event.getFinalDamage(), bow);
    if (damaged != null) getPlayerStat(damaged).onDamaged(event.getFinalDamage());
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

    if (victim.getSettings().getValue(SettingKey.STATS).equals(SettingValue.STATS_ON)) {
      PlayerStats victimStats = getPlayerStat(victim);

      victimStats.onDeath();

      sendPlayerStats(victim, victimStats);
    }

    if (murderer != null
        && PlayerRelation.get(victim.getParticipantState(), murderer) != PlayerRelation.ALLY
        && PlayerRelation.get(victim.getParticipantState(), murderer) != PlayerRelation.SELF
        && murderer.getSettings().getValue(SettingKey.STATS).equals(SettingValue.STATS_ON)) {

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
    if (stats.getHotbarTask() != null && !stats.getHotbarTask().isDone()) {
      stats.getHotbarTask().cancel(true);
    }
    stats.putHotbarTaskCache(sendLongHotbarMessage(player, stats.getBasicStatsMessage()));
  }

  private Future<?> sendLongHotbarMessage(MatchPlayer player, Component message) {
    Future<?> task =
        match
            .getExecutor(MatchScope.LOADED)
            .scheduleWithFixedDelay(() -> player.showHotbar(message), 0, 1, TimeUnit.SECONDS);

    match.getExecutor(MatchScope.LOADED).schedule(() -> task.cancel(true), 4, TimeUnit.SECONDS);

    return task;
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {

    if (allPlayerStats.isEmpty()) return;

    Map<UUID, Integer> allKills = new HashMap<>();
    Map<UUID, Integer> allKillstreaks = new HashMap<>();
    Map<UUID, Integer> allDeaths = new HashMap<>();
    Map<UUID, Integer> allBowshots = new HashMap<>();
    Map<UUID, Double> allDamage = new HashMap<>();

    for (Map.Entry<UUID, PlayerStats> mapEntry : allPlayerStats.entrySet()) {
      UUID playerUUID = mapEntry.getKey();
      PlayerStats playerStats = mapEntry.getValue();

      getPlayerStat(playerUUID);

      allKills.put(playerUUID, playerStats.getKills());
      allKillstreaks.put(playerUUID, playerStats.getMaxKillstreak());
      allDeaths.put(playerUUID, playerStats.getDeaths());
      allBowshots.put(playerUUID, playerStats.getLongestBowKill());
      allDamage.put(playerUUID, playerStats.getDamageDone());
    }

    Component killMessage = getMessage("match.stats.kills", sortStats(allKills), TextColor.GREEN);
    Component killstreakMessage =
        getMessage("match.stats.killstreak", sortStats(allKillstreaks), TextColor.GREEN);
    Component deathMessage = getMessage("match.stats.deaths", sortStats(allDeaths), TextColor.RED);
    Map.Entry<UUID, Integer> bestBowshot = sortStats(allBowshots);
    if (bestBowshot.getValue() == 1)
      bestBowshot.setValue(2); // Avoids translating "1 block" vs "n blocks"
    Component bowshotMessage = getMessage("match.stats.bowshot", bestBowshot, TextColor.YELLOW);
    Component damageMessage =
        getMessage("match.stats.damage", sortStatsDouble(allDamage), TextColor.GREEN);

    final Collection<Competitor> competitors = match.getCompetitors();

    boolean showAllVerboseStats =
        verboseStats && competitors.stream().allMatch(c -> c instanceof Team);

    if (showAllVerboseStats) {

      final List<InventoryMenuItem> items =
          competitors.stream()
              .map(c -> new TeamStatsInventoryMenuItem(match, c))
              .collect(Collectors.toList());

      endOfMatchMenu =
          competitors.size() <= 4
              ? InventoryMenuUtils.smallMenu(match, verboseStatsTitle, items)
              : InventoryMenuUtils.progressiveMenu(match, verboseStatsTitle, items);
    }

    InventoryMenuItem verboseStatsItem = new VerboseStatsInventoryMenuItem(endOfMatchMenu);

    match
        .getExecutor(MatchScope.LOADED)
        .schedule(
            () -> {
              for (MatchPlayer viewer : match.getPlayers()) {
                if (viewer.getSettings().getValue(SettingKey.STATS) == SettingValue.STATS_OFF)
                  continue;

                viewer.sendMessage(
                    TextFormatter.horizontalLineHeading(
                        viewer.getBukkit(),
                        TranslatableComponent.of("match.stats.title", TextColor.YELLOW),
                        TextColor.WHITE));
                viewer.sendMessage(killMessage);
                viewer.sendMessage(killstreakMessage);
                viewer.sendMessage(deathMessage);
                if (bestBowshot.getValue() != 0) viewer.sendMessage(bowshotMessage);
                if (verboseStats) viewer.sendMessage(damageMessage);
                if (showAllVerboseStats)
                  viewer.getInventory().setItem(7, verboseStatsItem.createItem(viewer));
              }
            },
            5 + 1, // NOTE: This is 1 second after the votebook appears
            TimeUnit.SECONDS);
  }

  @EventHandler
  public void onToolClick(PlayerInteractEvent event) {
    if (!verboseStats
        || !match.isFinished()
        || !match.getCompetitors().stream().allMatch(c -> c instanceof Team)) return;
    Action action = event.getAction();
    if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
      ItemStack item = event.getPlayer().getItemInHand();

      if (item.getType() == Material.RED_SANDSTONE) {
        MatchPlayer player = match.getPlayer(event.getPlayer());
        if (player == null) return;
        endOfMatchMenu.display(player);
      }
    }
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
    return TranslatableComponent.of(
        messageKey, playerName(mapEntry.getKey()), numberComponent(mapEntry.getValue(), color));
  }

  /**
   * Wraps a {@link Number} in a {@link TextComponent} that is bolded and colored with the given
   * {@link TextColor}.
   *
   * <p>If the given number is a double, it is formatted with {@link
   * StatsMatchModule#STATS_DECIMALFORMAT}.
   *
   * <p>If the number is NaN "-" is wrapped instead
   *
   * @param stat The number you want wrapped in a {@link Component}
   * @param color The color you want the number to be
   * @return A bolded {@link Component} wrapping the given number or "-" if NaN
   */
  public static Component numberComponent(Number stat, TextColor color) {
    double doubleStat = stat.doubleValue();
    String returnValue;
    if (Double.isNaN(doubleStat)) returnValue = "-"; // If NaN, dont try to display as a number
    else if (doubleStat % 1 == 0)
      returnValue =
          Integer.toString(
              stat.intValue()); // Check if the given number can be displayed as an integer
    else
      returnValue = STATS_DECIMALFORMAT.format(doubleStat); // If not, display as a formatted double
    return TextComponent.of(returnValue, color, TextDecoration.BOLD);
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
    return PlayerComponent.of(
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
