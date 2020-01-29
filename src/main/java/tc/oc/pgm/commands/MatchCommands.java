package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityGoal;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.util.components.ComponentUtils;
import tc.oc.util.components.PeriodFormats;
import tc.oc.util.localization.Locales;

public class MatchCommands {

  @Command(
      aliases = {"matchinfo", "match"},
      desc = "Shows information about the current match")
  public static void matchInfo(CommandSender sender, MatchPlayer player, Match match) {
    // indicates whether we have game information from the match yet
    boolean haveGameInfo =
        match.getPhase() == MatchPhase.RUNNING || match.getPhase() == MatchPhase.FINISHED;

    sender.sendMessage(
        ComponentUtils.horizontalLineHeading(
            ChatColor.YELLOW
                + AllTranslations.get()
                    .translate("command.match.matchInfo.title", sender, match.getId()),
            ChatColor.WHITE,
            ComponentUtils.MAX_CHAT_WIDTH));

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

    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    FreeForAllMatchModule ffamm = match.getModule(FreeForAllMatchModule.class);
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

    GoalMatchModule gmm = match.getModule(GoalMatchModule.class);
    if (haveGameInfo && gmm != null) {
      if (tmm != null && gmm.getGoalsByCompetitor().size() > 0) {
        Multimap<Team, String> teamGoalTexts = HashMultimap.create();

        for (Team team : tmm.getParticipatingTeams()) {
          for (Goal<?> goal : gmm.getGoals(team)) {
            if (goal.isVisible()) {
              if (player != null) {
                teamGoalTexts.put(
                    team, renderGoal(goal, player.getCompetitor(), player.getParty()));
              } else {
                teamGoalTexts.put(team, renderGoal(goal, null, match.getDefaultParty()));
              }
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
        ScoreMatchModule smm = match.getModule(ScoreMatchModule.class);
        if (smm != null) {
          sender.sendMessage(smm.getStatusMessage());
        }
      }
    }
  }

  // Modified from SidebarMatchModule to make formatting easier
  private static String renderGoal(
      Goal<?> goal, @Nullable Competitor competitor, Party viewingParty) {
    StringBuilder sb = new StringBuilder(" ");

    sb.append(goal.renderSidebarStatusColor(competitor, viewingParty));
    sb.append(goal.renderSidebarStatusText(competitor, viewingParty));

    if (goal instanceof ProximityGoal) {
      sb.append(" ");
      // Show teams their own proximity on shared goals
      sb.append(((ProximityGoal) goal).renderProximity(competitor, viewingParty));
    }

    sb.append(" ");
    sb.append(goal.renderSidebarLabelColor(competitor, viewingParty));
    sb.append(goal.renderSidebarLabelText(competitor, viewingParty));

    return sb.toString();
  }
}
