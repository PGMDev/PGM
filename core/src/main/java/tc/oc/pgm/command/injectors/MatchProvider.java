package tc.oc.pgm.command.injectors;

import static tc.oc.pgm.util.text.TextException.playerOnly;

import cloud.commandframework.annotations.AnnotationAccessor;
import cloud.commandframework.annotations.injection.ParameterInjector;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.CommandExecutionException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

public final class MatchProvider implements ParameterInjector<CommandSender, Match> {

  @Override
  public Match create(
      CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    final Match match = PGM.get().getMatchManager().getMatch(context.getSender());
    if (match != null) return match;
    throw new CommandExecutionException(playerOnly());
  }
}
