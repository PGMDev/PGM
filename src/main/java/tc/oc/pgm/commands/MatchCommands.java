package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.util.StringUtils;
import tc.oc.util.components.PeriodFormats;
import tc.oc.util.localization.Locales;

public class MatchCommands {

  @Command(
      aliases = {"matchinfo", "match"},
      desc = "Shows information about the current match")
  public static void matchInfo(CommandSender sender, Match match) {
    // indicates whether we have game information from the match yet
    boolean haveGameInfo =
        match.getPhase() == MatchPhase.RUNNING || match.getPhase() == MatchPhase.FINISHED;

    sender.sendMessage(
        StringUtils.dashedChatMessage(
            ChatColor.DARK_AQUA
                + " "
                + AllTranslations.get().translate("command.match.matchInfo.title", sender),
            ChatColor.STRIKETHROUGH + "-",
            ChatColor.RED.toString()));
    if (haveGameInfo) {
      // show match time
      sender.sendMessage(
          ChatColor.DARK_PURPLE
              + AllTranslations.get().translate("command.match.matchInfo.time", sender)
              + ": "
              + ChatColor.GOLD
              + PeriodFormats.COLONS_PRECISE
                  .withLocale(Locales.getLocale(sender))
                  .print(match.getDuration().toPeriod()));
    }

    TeamMatchModule tmm = match.getMatchModule(TeamMatchModule.class);
    FreeForAllMatchModule ffamm = match.getMatchModule(FreeForAllMatchModule.class);
    List<String> teamCountParts = Lists.newArrayList();

    if (tmm != null) {
      for (Team team : tmm.getTeams()) {
        StringBuilder msg = new StringBuilder();

        String teamName = team.getName();
        if (teamName.endsWith(" Team")) teamName = teamName.substring(0, teamName.length() - 5);

        msg.append(team.getColor())
            .append(teamName)
            .append(ChatColor.GRAY)
            .append(": ")
            .append(ChatColor.WHITE)
            .append(team.getPlayers().size());

        if (team.getMaxPlayers() != Integer.MAX_VALUE) {
          msg.append(ChatColor.GRAY).append("/").append(team.getMaxPlayers());
        }

        teamCountParts.add(msg.toString());
      }
    } else if (ffamm != null) {
      teamCountParts.add(
          ChatColor.YELLOW
              + AllTranslations.get().translate("command.match.matchInfo.players", sender)
              + ChatColor.GRAY
              + ": "
              + ChatColor.WHITE
              + match.getParticipants().size()
              + ChatColor.GRAY
              + '/'
              + ffamm.getMaxPlayers());
    }

    teamCountParts.add(
        ChatColor.AQUA
            + AllTranslations.get().translate("command.match.matchInfo.observers", sender)
            + ChatColor.GRAY
            + ": "
            + ChatColor.WHITE
            + match.getObservers().size());

    sender.sendMessage(Joiner.on(ChatColor.DARK_GRAY + " | ").join(teamCountParts));

    GoalMatchModule gmm = match.getMatchModule(GoalMatchModule.class);
    if (haveGameInfo && gmm != null) {
      if (tmm != null && gmm.getGoalsByCompetitor().size() > 0) {
        Multimap<Team, String> teamGoalTexts = HashMultimap.create();

        for (Team team : tmm.getParticipatingTeams()) {
          for (Goal goal : gmm.getGoals(team)) {
            if (goal.isVisible()) {
              teamGoalTexts.put(
                  team,
                  (goal.isCompleted(team) ? ChatColor.GREEN : ChatColor.DARK_RED) + goal.getName());
            }
          }
        }

        if (!teamGoalTexts.isEmpty()) {
          sender.sendMessage(
              ChatColor.DARK_PURPLE
                  + AllTranslations.get().translate("command.match.matchInfo.goals", sender)
                  + ":");

          for (Map.Entry<Team, Collection<String>> entry : teamGoalTexts.asMap().entrySet()) {
            Team team = entry.getKey();
            Collection<String> goalTexts = entry.getValue();

            sender.sendMessage(
                "  "
                    + team.getColoredName()
                    + ChatColor.GRAY
                    + ": "
                    + Joiner.on("  ").join(goalTexts));
          }
        }
      } else {
        // FIXME: this is not the best way to handle scores
        ScoreMatchModule smm = match.getMatchModule(ScoreMatchModule.class);
        if (smm != null) {
          sender.sendMessage(smm.getStatusMessage());
        }
      }
    }
  }
}
