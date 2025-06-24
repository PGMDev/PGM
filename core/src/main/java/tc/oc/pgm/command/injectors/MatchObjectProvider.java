package tc.oc.pgm.command.injectors;

import static tc.oc.pgm.util.text.TextException.playerOnly;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.injection.ParameterInjector;
import org.incendo.cloud.util.annotation.AnnotationAccessor;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.command.util.CommandUtils;
import tc.oc.pgm.util.text.TextException;

public abstract class MatchObjectProvider<T> implements ParameterInjector<CommandSender, T> {
  @Override
  public T create(
      @NotNull CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    final Match match = CommandUtils.getMatch(context);
    if (match != null) {
      T value = get(match);
      if (value != null) return value;
    }

    throw new CommandExecutionException(missingException());
  }

  protected abstract T get(Match match);

  protected TextException missingException() {
    return playerOnly();
  }
}
