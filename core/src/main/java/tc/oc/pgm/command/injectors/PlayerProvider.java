package tc.oc.pgm.command.injectors;

import static tc.oc.pgm.util.text.TextException.playerOnly;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.injection.ParameterInjector;
import org.incendo.cloud.util.annotation.AnnotationAccessor;
import org.jetbrains.annotations.NotNull;

public final class PlayerProvider implements ParameterInjector<CommandSender, Player> {

  @Override
  public Player create(
      CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    CommandSender sender = context.sender();
    if (sender instanceof Player) return (Player) sender;

    throw new CommandExecutionException(playerOnly());
  }
}
