package tc.oc.pgm.command.graph;

import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.text.TextException;

final class TeamsProvider implements BukkitProvider<TeamMatchModule> {

  @Override
  public boolean isProvided() {
    return true;
  }

  @Override
  public TeamMatchModule get(
      CommandSender sender, CommandArgs commandArgs, List<? extends Annotation> list) {
    final Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match != null) {
      final TeamMatchModule teams = match.getModule(TeamMatchModule.class);
      if (teams != null) {
        return teams;
      }
    }

    throw TextException.of("command.noTeams");
  }
}
