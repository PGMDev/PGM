package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityGoal;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.goals.TouchableGoal;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextTranslations;

// TODO: make the output nicer and translate
public final class ProximityCommand {

  @Command(
      aliases = {"proximity", "prox"},
      desc = "Show the progress of each objective")
  public void proximity(MatchPlayer player, Match match) {
    if (player != null && player.isParticipating()) {
      throw TextException.noPermission();
    }

    // TODO: use components
    List<String> lines = new ArrayList<>();

    for (Team team : match.needModule(TeamMatchModule.class).getParticipatingTeams()) {
      boolean teamHeader = false;
      final GoalMatchModule gmm = match.needModule(GoalMatchModule.class);

      for (Goal<?> goal : gmm.getGoals(team)) {
        if (goal instanceof TouchableGoal && goal.isVisible()) {
          TouchableGoal touchable = (TouchableGoal) goal;
          ProximityGoal proximity = (ProximityGoal) goal;

          if (!teamHeader) {
            lines.add(TextTranslations.translateLegacy(team.getName(), player.getBukkit()));
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
      throw TextException.of("command.emptyResult");
    }

    for (String line : lines) {
      player.sendMessage(line);
    }
  }
}
