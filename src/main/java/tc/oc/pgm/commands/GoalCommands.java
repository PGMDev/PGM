package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.goals.*;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

public class GoalCommands {

  @Command(
      aliases = {"proximity"},
      desc = "Show stats about how close each competitor has been to each objective")
  public static void proximity(CommandSender sender, MatchPlayer matchPlayer, Match match)
      throws CommandException {
    TeamMatchModule tmm = match.needModule(TeamMatchModule.class);

    if (matchPlayer != null && matchPlayer.isParticipating()) {
      throw new CommandException("The /proximity command is only available to observers");
    }

    List<String> lines = new ArrayList<>();

    for (Team team : tmm.getParticipatingTeams()) {
      boolean teamHeader = false;
      final GoalMatchModule gmm = match.needModule(GoalMatchModule.class);

      for (Goal<?> goal : gmm.getGoals(team)) {
        if (goal instanceof TouchableGoal && goal.isVisible()) {
          TouchableGoal touchable = (TouchableGoal) goal;
          ProximityGoal proximity = (ProximityGoal) goal;

          if (!teamHeader) {
            lines.add(team.getColoredName());
            teamHeader = true;
          }

          String line = ChatColor.WHITE + "  " + touchable.getColoredName() + ChatColor.WHITE;

          if (touchable.isCompleted(team)) {
            line += ChatColor.GREEN + " COMPLETE";
          } else if (touchable.hasTouched(team)) {
            line += ChatColor.YELLOW + " TOUCHED";
          } else {
            line += ChatColor.RED + " UNTOUCHED";
          }

          if (proximity.isProximityRelevant(team)) {
            ProximityMetric metric = proximity.getProximityMetric(team);
            if (metric != null) {
              line +=
                  ChatColor.GRAY
                      + " "
                      + metric.description()
                      + ": "
                      + ChatColor.AQUA
                      + String.format("%.2f", proximity.getMinimumDistance(team));
            }
          }

          lines.add(line);
        }
      }
    }

    if (lines.isEmpty()) {
      sender.sendMessage(ChatColor.RED + "There are no objectives that track proximity");
    } else {
      sender.sendMessage(lines.toArray(new String[0]));
    }
  }
}
