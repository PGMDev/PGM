package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.noPermission;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityGoal;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.goals.TouchableGoal;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.text.TextTranslations;

// TODO: make the output nicer and translate
public final class ProximityCommand {

  @CommandMethod("proximity|prox")
  @CommandDescription("Show the progress of each objective")
  public void proximity(MatchPlayer player, TeamMatchModule tmm, GoalMatchModule gmm) {
    if (player != null && player.isParticipating()) {
      throw noPermission();
    }

    // TODO: use components
    List<String> lines = new ArrayList<>();

    for (Team team : tmm.getParticipatingTeams()) {
      boolean teamHeader = false;

      for (Goal<?> goal : gmm.getGoals(team)) {
        if (goal instanceof TouchableGoal && goal.hasShowOption(ShowOption.SHOW_INFO)) {
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
      throw exception("command.emptyResult");
    }

    for (String line : lines) {
      player.sendMessage(text(line));
    }
  }
}
