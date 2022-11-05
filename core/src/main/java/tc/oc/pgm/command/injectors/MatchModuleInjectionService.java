package tc.oc.pgm.command.injectors;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import cloud.commandframework.annotations.AnnotationAccessor;
import cloud.commandframework.annotations.injection.InjectionService;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.types.tuples.Triplet;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.command.util.CommandUtils;

public class MatchModuleInjectionService implements InjectionService<CommandSender> {

  @Override
  public @Nullable Object handle(
      @NonNull Triplet<CommandContext<CommandSender>, Class<?>, AnnotationAccessor> param)
      throws Exception {
    // Not a match module? we do not handle that.
    Class<?> cls = param.getSecond();
    if (!MatchModule.class.isAssignableFrom(cls)) return null;

    final Match match = CommandUtils.getMatch(param.getFirst());
    if (match == null) throw new CommandExecutionException(playerOnly());

    @SuppressWarnings("unchecked")
    MatchModule mm = match.getModule((Class<? extends MatchModule>) cls);
    if (mm == null)
      throw new CommandExecutionException(
          exception(
              "command.moduleNotFound", text(cls.getSimpleName().replace("MatchModule", ""))));

    return mm;
  }
}
