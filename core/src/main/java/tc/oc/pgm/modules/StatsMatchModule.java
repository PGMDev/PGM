package tc.oc.pgm.modules;

import java.math.BigDecimal;
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
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.tracker.damage.ProjectileInfo;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.ComponentUtils;
import tc.oc.util.bukkit.component.Components;
import tc.oc.util.bukkit.component.types.PersonalizedText;
import tc.oc.util.bukkit.component.types.PersonalizedTranslatable;
import tc.oc.util.bukkit.translations.AllTranslations;

@ListenerScope(MatchScope.RUNNING)
public class StatsMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<StatsMatchModule> {

    @Override
    public StatsMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new StatsMatchModule();
    }
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
      if (new BigDecimal(distance).compareTo(new BigDecimal(longestBowKill)) > 0) {
        longestBowKill = (int) distance;
      }
    }

    private final DecimalFormat decimalFormatKd = new DecimalFormat("#.##");

    Component getBasicStatsMessage() {
      String kd;
      if (deaths == 0) {
        kd = "0";
      } else {
        kd = decimalFormatKd.format(kills / deaths);
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

  private static final Map<UUID, PlayerStats> allPlayerStats = new HashMap<>();

  @EventHandler
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    Match match = event.getMatch();
    MatchPlayer victim = event.getVictim();
    MatchPlayer murderer = null;

    if (event.getKiller() != null)
      murderer = event.getKiller().getParty().getPlayer(event.getKiller().getId());

    UUID victimUUID = victim.getId();
    PlayerStats victimStats = allPlayerStats.get(victimUUID);

    if (PlayerStatsDoesNotExist(victimUUID)) victimStats = putNewPlayer(victimUUID);

    victimStats.onDeath();

    sendLongHotbarMessage(victim, match, victimStats.getBasicStatsMessage());

    if (murderer != null
        && PlayerRelation.get(victim.getParticipantState(), murderer) != PlayerRelation.ALLY
        && PlayerRelation.get(victim.getParticipantState(), murderer) != PlayerRelation.SELF) {
      UUID murdererUUID = murderer.getId();
      PlayerStats murdererStats = allPlayerStats.get(murdererUUID);

      if (PlayerStatsDoesNotExist(murdererUUID)) murdererStats = putNewPlayer(murdererUUID);

      if (event.getDamageInfo() instanceof ProjectileInfo) {
        murdererStats.setLongestBowKill(
            victim
                .getState()
                .getLocation()
                .distance(((ProjectileInfo) event.getDamageInfo()).getOrigin()));
      }

      murdererStats.onMurder();

      sendLongHotbarMessage(murderer, match, murdererStats.getBasicStatsMessage());
    }
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {
    Match match = event.getMatch();
    Map<UUID, Integer> allKills = new HashMap<>();
    Map<UUID, Integer> allKillstreaks = new HashMap<>();
    Map<UUID, Integer> allDeaths = new HashMap<>();
    Map<UUID, Integer> allBowshots = new HashMap<>();

    for (Map.Entry<UUID, PlayerStats> mapEntry : allPlayerStats.entrySet()) {
      UUID playerUUID = mapEntry.getKey();
      PlayerStats playerStats = mapEntry.getValue();

      if (PlayerStatsDoesNotExist(playerUUID)) playerStats = putNewPlayer(playerUUID);

      allKills.put(playerUUID, playerStats.kills);
      allKillstreaks.put(playerUUID, playerStats.killstreakMax);
      allDeaths.put(playerUUID, playerStats.deaths);
      allBowshots.put(playerUUID, playerStats.longestBowKill);
    }

    Component killMessage = getMessage("stats.kills", sortStats(allKills), match, ChatColor.GREEN);
    Component killstreakMessage =
        getMessage("stats.killstreak", sortStats(allKillstreaks), match, ChatColor.GREEN);
    Component deathMessage = getMessage("stats.death", sortStats(allDeaths), match, ChatColor.RED);
    TopResult bestBowshot = sortStats(allBowshots);
    Component bowshotMessage = getMessage("stats.bowshot", bestBowshot, match, ChatColor.YELLOW);

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
      if (bestBowshot.stat != 0) viewer.sendMessage(bowshotMessage);
    }
  }

  private static class TopResult {
    UUID uuid;
    int stat;
  }

  private TopResult sortStats(Map<UUID, Integer> map) {
    Map.Entry<UUID, Integer> mapEntry =
        map.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).orElse(null);
    TopResult topResult = new TopResult();

    if (mapEntry == null) { // Should never happen, but acts as a failsafe
      topResult.uuid = UUID.fromString("3c7db14d-ac4b-4e35-b2c6-3b2237f382be");
      topResult.stat = 0;
      return topResult;
    }

    topResult.uuid = mapEntry.getKey();
    topResult.stat = mapEntry.getValue();

    return topResult;
  }

  private void sendLongHotbarMessage(MatchPlayer player, Match match, Component message) {
    int taskId =
        match
            .getScheduler(MatchScope.LOADED)
            .runTaskTimer(
                0,
                5,
                () -> {
                  player.sendHotbarMessage(message);
                })
            .getTaskId();

    match
        .getScheduler(MatchScope.LOADED)
        .runTaskLater(
            20 * 4,
            () -> {
              Bukkit.getScheduler().cancelTask(taskId);
            });
  }

  Component getMessage(String messageKey, TopResult topResult, Match match, ChatColor color) {
    return new Component(
        new PersonalizedTranslatable(
                messageKey,
                playerName(match, topResult.uuid),
                new PersonalizedText(Integer.toString(topResult.stat), color).render())
            .render());
  }

  private PersonalizedText playerName(Match match, UUID playerUUID) {
    if (Bukkit.getPlayer(playerUUID) == null) {
      if (Bukkit.getOfflinePlayer(playerUUID).getName() == null) {
        return new PersonalizedText("Noone", ChatColor.MAGIC, ChatColor.BLACK);
      }
      return new PersonalizedText(
          Bukkit.getOfflinePlayer(playerUUID).getName(), ChatColor.DARK_AQUA);
    }
    return new PersonalizedText(match.getPlayer(playerUUID).getBukkit().getDisplayName());
  }

  public static boolean PlayerStatsDoesNotExist(UUID player) {
    return allPlayerStats.get(player) == null;
  }

  private static PlayerStats putNewPlayer(UUID player) {
    allPlayerStats.put(player, new PlayerStats());
    return allPlayerStats.get(player);
  }

  public static Component getBasicStatsMessage(UUID player) {
    if (PlayerStatsDoesNotExist(player)) return putNewPlayer(player).getBasicStatsMessage();
    return allPlayerStats.get(player).getBasicStatsMessage();
  }
}
