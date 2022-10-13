package tc.oc.pgm.command.injectors;

import static tc.oc.pgm.util.text.TextException.playerOnly;

import cloud.commandframework.annotations.AnnotationAccessor;
import cloud.commandframework.annotations.injection.ParameterInjector;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.CommandExecutionException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public final class MatchPlayerProvider implements ParameterInjector<CommandSender, MatchPlayer> {

  @Override
  public MatchPlayer create(
      CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    CommandSender sender = context.getSender();
    if (sender instanceof Player) {
      final MatchPlayer player = PGM.get().getMatchManager().getPlayer((Player) sender);
      if (player != null) return player;
    }

    throw new CommandExecutionException(playerOnly());
  }
}
