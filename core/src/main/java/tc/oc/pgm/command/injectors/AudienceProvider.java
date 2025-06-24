package tc.oc.pgm.command.injectors;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.injection.ParameterInjector;
import org.incendo.cloud.util.annotation.AnnotationAccessor;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.Audience;

public final class AudienceProvider implements ParameterInjector<CommandSender, Audience> {

  @Override
  public @NotNull Audience create(
      CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    return Audience.get(context.sender());
  }
}
