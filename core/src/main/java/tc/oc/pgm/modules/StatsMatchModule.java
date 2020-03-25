package tc.oc.pgm.modules;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.ComponentUtils;
import tc.oc.util.bukkit.component.Components;
import tc.oc.util.bukkit.component.types.PersonalizedText;
import tc.oc.util.bukkit.component.types.PersonalizedTranslatable;
import tc.oc.util.bukkit.translations.AllTranslations;

@ListenerScope(MatchScope.RUNNING)
public class StatsMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Map<UUID, PlayerStats> allPlayerStats = new HashMap<>();

  public StatsMatchModule(Match match) {
    this.match = match;
  }

  public static class PlayerStats {
    private int kills;
    private int deaths;
    private int killstreak;
    private int killstreakMax;
    private int longestBowKill;

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

    Component getBasicStatsMessage() {
      String kd;
      if (deaths == 0) {
        kd = "0";
      } else {
        kd = decimalFormatKd.format(kills / (double) deaths);
      }
      return new Component(
          new PersonalizedTranslatable(
                  "stats.basic",
                  new PersonalizedText(Integer.toString(kills), ChatColor.GREEN),
                  new PersonalizedText(Integer.toString(killstreak), ChatColor.GREEN),
                  new PersonalizedText(Integer.toString(deaths), ChatColor.RED),
                  new PersonalizedText(kd, ChatColor.GREEN))
              .render());
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

      sendLongHotbarMessage(victim, victimStats.getBasicStatsMessage());
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

      sendLongHotbarMessage(murderer, murdererStats.getBasicStatsMessage());
    }
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {
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

    Component killMessage = getMessage("stats.kills", sortStats(allKills), ChatColor.GREEN);
    Component killstreakMessage =
        getMessage("stats.killstreak", sortStats(allKillstreaks), ChatColor.GREEN);
    Component deathMessage = getMessage("stats.deaths", sortStats(allDeaths), ChatColor.RED);
    Map.Entry<UUID, Integer> bestBowshot = sortStats(allBowshots);
    String bowMessageKey =
        (bestBowshot.getValue() == 1) ? "stats.bowshot.block" : "stats.bowshot.blocks";
    Component bowshotMessage = getMessage(bowMessageKey, bestBowshot, ChatColor.YELLOW);

    for (MatchPlayer viewer : match.getPlayers()) {
      viewer.sendMessage(
          Components.fromLegacyText(
              ComponentUtils.horizontalLineHeading(
                  ChatColor.YELLOW
                      + AllTranslations.get().translate("stats.best", viewer.getBukkit()),
                  ChatColor.WHITE,
                  ComponentUtils.MAX_CHAT_WIDTH)));

      viewer.sendMessage(killMessage);
      viewer.sendMessage(killstreakMessage);
      viewer.sendMessage(deathMessage);
      if (bestBowshot.getValue() != 0) viewer.sendMessage(bowshotMessage);
    }
  }

  private Map.Entry<UUID, Integer> sortStats(Map<UUID, Integer> map) {
    return map.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).orElse(null);
  }

  private void sendLongHotbarMessage(MatchPlayer player, Component message) {
    int taskId =
        match
            .getScheduler(MatchScope.LOADED)
            .runTaskTimer(0, 5, () -> player.sendHotbarMessage(message))
            .getTaskId();

    match
        .getScheduler(MatchScope.LOADED)
        .runTaskLater(20 * 4, () -> Bukkit.getScheduler().cancelTask(taskId));
  }

  Component getMessage(String messageKey, Map.Entry<UUID, Integer> mapEntry, ChatColor color) {
    return new Component(
        new PersonalizedTranslatable(
                messageKey,
                playerName(mapEntry.getKey()),
                new PersonalizedText(Integer.toString(mapEntry.getValue()), color).render())
            .render());
  }

  private PersonalizedText playerName(UUID playerUUID) {
    if (Bukkit.getPlayer(playerUUID) == null) {
      if (Bukkit.getOfflinePlayer(playerUUID).getName() == null) {
        return new PersonalizedText("Unknown", ChatColor.MAGIC, ChatColor.BLACK);
      }
      return new PersonalizedText(
          Bukkit.getOfflinePlayer(playerUUID).getName(), ChatColor.DARK_AQUA);
    }
    return new PersonalizedText(match.getPlayer(playerUUID).getBukkit().getDisplayName());
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
