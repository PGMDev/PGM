package tc.oc.pgm.command.injectors;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.injection.InjectionRequest;
import org.incendo.cloud.injection.InjectionService;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.command.util.CommandUtils;

public class MatchModuleInjectionService implements InjectionService<CommandSender> {

  @Override
  public @Nullable Object handle(@NonNull InjectionRequest<CommandSender> param) throws Exception {
    // Not a match module? we do not handle that.
    Class<?> cls = param.injectedClass();
    if (!MatchModule.class.isAssignableFrom(cls)) return null;

    final Match match = CommandUtils.getMatch(param.commandContext());
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
