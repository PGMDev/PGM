package tc.oc.pgm.command.injectors;

import static tc.oc.pgm.util.text.TextException.playerOnly;

import cloud.commandframework.annotations.AnnotationAccessor;
import cloud.commandframework.annotations.injection.ParameterInjector;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.CommandExecutionException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PlayerProvider implements ParameterInjector<CommandSender, Player> {

  @Override
  public Player create(
      CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    CommandSender sender = context.getSender();
    if (sender instanceof Player) return (Player) sender;

    throw new CommandExecutionException(playerOnly());
  }
}
