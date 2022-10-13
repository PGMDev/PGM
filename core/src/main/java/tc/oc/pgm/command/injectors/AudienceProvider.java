package tc.oc.pgm.command.injectors;

import cloud.commandframework.annotations.AnnotationAccessor;
import cloud.commandframework.annotations.injection.ParameterInjector;
import cloud.commandframework.context.CommandContext;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.Audience;

public final class AudienceProvider implements ParameterInjector<CommandSender, Audience> {

  @Override
  public @NotNull Audience create(
      CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    return Audience.get(context.getSender());
  }
}
