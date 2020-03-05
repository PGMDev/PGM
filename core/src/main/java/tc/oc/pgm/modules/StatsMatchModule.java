package tc.oc.pgm.modules;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import net.md_5.bungee.api.ChatColor;
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
import tc.oc.util.bukkit.nms.NMSHacks;

@ListenerScope(MatchScope.RUNNING)
public class StatsMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<StatsMatchModule> {

    @Override
    public StatsMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new StatsMatchModule();
    }
  }

  public static class playerStats {
    private int kills = 0;
    private int deaths = 0;
    private int killstreak = 0;
    private int killstreakMax = 0;
    private int longestBowKill = 0;

    public void onMurder() {
      kills++;
      killstreak++;
      if (killstreak > killstreakMax) killstreakMax = killstreak;
    }

    public void onDeath() {
      deaths++;
      killstreak = 0;
    }

    public void setLongestBowKill(double distance) {
      if (distance > longestBowKill) {
        longestBowKill = Integer.parseInt(new DecimalFormat("#").format(Math.round(distance)));
      }
    }

    public Component getBasicStatsMessage() {
      String KD;
      if (deaths == 0) {
        KD = "0";
      } else {
        DecimalFormat df = new DecimalFormat("#.##");
        KD = df.format(kills / deaths);
      }
      return new Component(
          new PersonalizedTranslatable(
                  "stats.basic",
                  new PersonalizedText(Integer.toString(kills), ChatColor.GREEN),
                  new PersonalizedText(Integer.toString(killstreak), ChatColor.GREEN),
                  new PersonalizedText(Integer.toString(deaths), ChatColor.RED),
                  new PersonalizedText(KD, ChatColor.GREEN))
              .render());
    }
  }

  public static Map<String, playerStats> allPlayerStats;

  public StatsMatchModule() {
    allPlayerStats = new HashMap<>();
  }

  @EventHandler
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    MatchPlayer victim = event.getVictim();
    MatchPlayer murderer = null;
    try {
      murderer = event.getKiller().getParty().getPlayer(event.getKiller().getId());
    } catch (NullPointerException ignored) {
    }

    playerStats victimStats = allPlayerStats.get(event.getVictim().getBukkit().getName());
    if (victimStats == null) {
      allPlayerStats.put(victim.getBukkit().getName(), new playerStats());
      victimStats = allPlayerStats.get(event.getVictim().getBukkit().getName());
    }
    victimStats.onDeath();

    playerStats finalVictimStats = victimStats;
    // Without this delay the "Press LShift to dismount" message from the death minecart will
    // override the stats message.
    event
        .getMatch()
        .getScheduler(MatchScope.LOADED)
        .runTaskLater(
            0,
            () -> {
              NMSHacks.sendHotbarMessage(
                  victim.getBukkit(), finalVictimStats.getBasicStatsMessage());
            });

    if (murderer != null
        && PlayerRelation.get(victim.getParticipantState(), murderer.getState())
            != PlayerRelation.ALLY
        && PlayerRelation.get(victim.getParticipantState(), murderer) != PlayerRelation.SELF) {
      playerStats murdererStats = allPlayerStats.get(murderer.getBukkit().getName());
      if (murdererStats == null) {
        allPlayerStats.put(murderer.getBukkit().getName(), new playerStats());
        murdererStats = allPlayerStats.get(murderer.getBukkit().getName());
      }
      if (event.getDamageInfo() instanceof ProjectileInfo) {
        murdererStats.setLongestBowKill(
            victim
                .getState()
                .getLocation()
                .distance(((ProjectileInfo) event.getDamageInfo()).getOrigin()));
        murdererStats.onMurder();
        NMSHacks.sendHotbarMessage(
            murderer.getBukkit().getPlayer(), murdererStats.getBasicStatsMessage());
      }
    }
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {
    Match match = event.getMatch();
    HashMap<String, Integer> allKills = new HashMap<>();
    HashMap<String, Integer> allKillstreaks = new HashMap<>();
    HashMap<String, Integer> allDeaths = new HashMap<>();
    HashMap<String, Integer> allBowshots = new HashMap<>();
    for (MatchPlayer player : match.getPlayers()) {
      allKills.put(
          player.getBukkit().getDisplayName(),
          allPlayerStats.get(player.getBukkit().getName()).kills);
      allKillstreaks.put(
          player.getBukkit().getDisplayName(),
          allPlayerStats.get(player.getBukkit().getName()).killstreakMax);
      allDeaths.put(
          player.getBukkit().getDisplayName(),
          allPlayerStats.get(player.getBukkit().getName()).deaths);
      allBowshots.put(
          player.getBukkit().getDisplayName(),
          allPlayerStats.get(player.getBukkit().getName()).longestBowKill);
    }
    Component killMessage = getKillsMessage(sortStats(allKills));
    Component killstreakMessage = getKillstreakMessage(sortStats(allKillstreaks));
    Component deathMessage = getDeathsMessage(sortStats(allDeaths));
    Map.Entry<String, Integer> bestBowshot = sortStats(allBowshots);
    Component bowshotMessage = getBowshotMessage(bestBowshot);

    for (MatchPlayer viewer : match.getPlayers()) {
      viewer.sendMessage(
          Components.fromLegacyText(
              ComponentUtils.horizontalLineHeading(
                  ChatColor.YELLOW + "Best stats this match",
                  ChatColor.WHITE,
                  ComponentUtils.MAX_CHAT_WIDTH)));

      viewer.sendMessage(killMessage);
      viewer.sendMessage(killstreakMessage);
      viewer.sendMessage(deathMessage);
      if (bestBowshot.getValue()
          != 0) { // Prevent this from showing if bows are not used on the map played
        viewer.sendMessage(bowshotMessage);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Map.Entry<String, Integer> sortStats(HashMap<String, Integer> map) {
    Object[] a = map.entrySet().toArray();
    Arrays.sort(
        a,
        (Comparator)
            (o1, o2) ->
                ((Map.Entry<String, Integer>) o2)
                    .getValue()
                    .compareTo(((Map.Entry<String, Integer>) o1).getValue()));
    return (Map.Entry<String, Integer>) a[0];
  }

  Component getKillsMessage(Map.Entry<String, Integer> topResult) {
    return new Component(
        new PersonalizedTranslatable(
                "stats.kills",
                new PersonalizedText(topResult.getKey()),
                new PersonalizedText(Integer.toString(topResult.getValue()), ChatColor.GREEN))
            .render());
  }

  Component getKillstreakMessage(Map.Entry<String, Integer> topResult) {
    return new Component(
        new PersonalizedTranslatable(
                "stats.killstreak",
                new PersonalizedText(topResult.getKey()),
                new PersonalizedText(Integer.toString(topResult.getValue()), ChatColor.GREEN))
            .render());
  }

  Component getDeathsMessage(Map.Entry<String, Integer> topResult) {
    return new Component(
        new PersonalizedTranslatable(
                "stats.deaths",
                new PersonalizedText(topResult.getKey()),
                new PersonalizedText(Integer.toString(topResult.getValue()), ChatColor.RED))
            .render());
  }

  Component getBowshotMessage(Map.Entry<String, Integer> topResult) {
    return new Component(
        new PersonalizedTranslatable(
                "stats.bowshot",
                new PersonalizedText(topResult.getKey()),
                new PersonalizedText(Integer.toString(topResult.getValue()), ChatColor.YELLOW))
            .render());
  }
}
