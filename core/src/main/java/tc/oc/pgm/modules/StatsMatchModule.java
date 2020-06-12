package tc.oc.pgm.modules;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.tracker.info.ProjectileInfo;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.types.PlayerComponent;

@ListenerScope(MatchScope.RUNNING)
public class StatsMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Map<UUID, PlayerStats> allPlayerStats = new HashMap<>();
  // Since Bukkit#getOfflinePlayer reads the cached user files, and those files have an expire date
  // + will be wiped if X amount of players join, we need a seperate cache for players with stats
  private final Map<UUID, String> cachedUsernames = new HashMap<>();

  public StatsMatchModule(Match match) {
    this.match = match;
  }

  public static class PlayerStats {
    private int kills;
    private int deaths;
    private int killstreak;
    private int killstreakMax;
    private int longestBowKill;
    private Future<?> task;

    private void onMurder() {
      kills++;
      killstreak++;
      if (killstreak > killstreakMax) killstreakMax = killstreak;
    }

    private void onDeath() {
      deaths++;
      killstreak = 0;
    }

    private void setLongestBowKill(double distance) {
      if (distance > longestBowKill) {
        longestBowKill = (int) Math.ceil(distance);
      }
    }

    private final DecimalFormat decimalFormatKd = new DecimalFormat("#.##");

    public Component getBasicStatsMessage() {
      String kd;
      if (deaths == 0) {
        kd = Double.toString(kills);
      } else {
        kd = decimalFormatKd.format(kills / (double) deaths);
      }
      return TranslatableComponent.of(
          "match.stats",
          TextColor.GRAY,
          TextComponent.of(Integer.toString(kills), TextColor.GREEN),
          TextComponent.of(Integer.toString(killstreak), TextColor.GREEN),
          TextComponent.of(Integer.toString(deaths), TextColor.RED),
          TextComponent.of(kd, TextColor.GREEN));
    }
  }

  @EventHandler
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    MatchPlayer victim = event.getVictim();
    MatchPlayer murderer = null;

    if (event.getKiller() != null)
      murderer = event.getKiller().getParty().getPlayer(event.getKiller().getId());

    if (victim.getSettings().getValue(SettingKey.STATS).equals(SettingValue.STATS_ON)) {
      UUID victimUUID = victim.getId();
      PlayerStats victimStats = allPlayerStats.get(victimUUID);

      if (hasNoStats(victimUUID)) victimStats = putNewPlayer(victimUUID);

      victimStats.onDeath();

      sendPlayerStats(victim, victimStats);
    }

    if (murderer != null
        && PlayerRelation.get(victim.getParticipantState(), murderer) != PlayerRelation.ALLY
        && PlayerRelation.get(victim.getParticipantState(), murderer) != PlayerRelation.SELF
        && murderer.getSettings().getValue(SettingKey.STATS).equals(SettingValue.STATS_ON)) {
      UUID murdererUUID = murderer.getId();
      PlayerStats murdererStats = allPlayerStats.get(murdererUUID);

      if (hasNoStats(murdererUUID)) murdererStats = putNewPlayer(murdererUUID);

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

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {

    if (allPlayerStats.isEmpty()) return;

    Map<UUID, Integer> allKills = new HashMap<>();
    Map<UUID, Integer> allKillstreaks = new HashMap<>();
    Map<UUID, Integer> allDeaths = new HashMap<>();
    Map<UUID, Integer> allBowshots = new HashMap<>();

    for (Map.Entry<UUID, PlayerStats> mapEntry : allPlayerStats.entrySet()) {
      UUID playerUUID = mapEntry.getKey();
      PlayerStats playerStats = mapEntry.getValue();

      if (hasNoStats(playerUUID)) playerStats = putNewPlayer(playerUUID);

      allKills.put(playerUUID, playerStats.kills);
      allKillstreaks.put(playerUUID, playerStats.killstreakMax);
      allDeaths.put(playerUUID, playerStats.deaths);
      allBowshots.put(playerUUID, playerStats.longestBowKill);
    }

    Component killMessage = getMessage("match.stats.kills", sortStats(allKills), TextColor.GREEN);
    Component killstreakMessage =
        getMessage("match.stats.killstreak", sortStats(allKillstreaks), TextColor.GREEN);
    Component deathMessage = getMessage("match.stats.deaths", sortStats(allDeaths), TextColor.RED);
    Map.Entry<UUID, Integer> bestBowshot = sortStats(allBowshots);
    if (bestBowshot.getValue() == 1)
      bestBowshot.setValue(2); // Avoids translating "1 block" vs "n blocks"
    Component bowshotMessage = getMessage("match.stats.bowshot", bestBowshot, TextColor.YELLOW);

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
                        TranslatableComponent.of("match.stats.overall", TextColor.YELLOW),
                        TextColor.WHITE));
                viewer.sendMessage(killMessage);
                viewer.sendMessage(killstreakMessage);
                viewer.sendMessage(deathMessage);
                if (bestBowshot.getValue() != 0) viewer.sendMessage(bowshotMessage);
              }
            },
            5 + 1, // NOTE: This is 1 second after the votebook appears
            TimeUnit.SECONDS);
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

  private Map.Entry<UUID, Integer> sortStats(Map<UUID, Integer> map) {
    return map.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).orElse(null);
  }

  private void sendPlayerStats(MatchPlayer player, PlayerStats stats) {
    if (stats.task != null && !stats.task.isDone()) {
      stats.task.cancel(true);
    }
    stats.task = sendLongHotbarMessage(player, stats.getBasicStatsMessage());
  }

  private Future<?> sendLongHotbarMessage(MatchPlayer player, Component message) {
    Future<?> task =
        match
            .getExecutor(MatchScope.LOADED)
            .scheduleWithFixedDelay(() -> player.showHotbar(message), 0, 1, TimeUnit.SECONDS);

    match.getExecutor(MatchScope.LOADED).schedule(() -> task.cancel(true), 4, TimeUnit.SECONDS);

    return task;
  }

  Component getMessage(String messageKey, Map.Entry<UUID, Integer> mapEntry, TextColor color) {
    return TranslatableComponent.of(
        messageKey,
        playerName(mapEntry.getKey()),
        TextComponent.of(Integer.toString(mapEntry.getValue()), color, TextDecoration.BOLD));
  }

  private Component playerName(UUID playerUUID) {
    return PlayerComponent.of(
        Bukkit.getPlayer(playerUUID),
        cachedUsernames.getOrDefault(playerUUID, "Unknown"),
        NameStyle.FANCY);
  }

  public boolean hasNoStats(UUID player) {
    return allPlayerStats.get(player) == null;
  }

  private PlayerStats putNewPlayer(UUID player) {
    allPlayerStats.put(player, new PlayerStats());
    return allPlayerStats.get(player);
  }

  public Component getBasicStatsMessage(UUID player) {
    if (hasNoStats(player)) return putNewPlayer(player).getBasicStatsMessage();
    return allPlayerStats.get(player).getBasicStatsMessage();
  }
}
