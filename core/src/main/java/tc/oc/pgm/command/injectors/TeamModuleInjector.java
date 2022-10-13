package tc.oc.pgm.command.injectors;

import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.AnnotationAccessor;
import cloud.commandframework.annotations.injection.ParameterInjector;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.CommandExecutionException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.teams.TeamMatchModule;

public final class TeamModuleInjector implements ParameterInjector<CommandSender, TeamMatchModule> {

  public TeamMatchModule create(
      CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    final Match match = PGM.get().getMatchManager().getMatch(context.getSender());
    if (match != null) {
      final TeamMatchModule teams = match.getModule(TeamMatchModule.class);
      if (teams != null) {
        return teams;
      }
    }

    throw new CommandExecutionException(exception("command.noTeams"));
  }
}
