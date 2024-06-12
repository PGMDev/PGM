package tc.oc.pgm.command.injectors;

import static tc.oc.pgm.util.text.TextException.playerOnly;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.injection.ParameterInjector;
import org.incendo.cloud.util.annotation.AnnotationAccessor;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public final class MatchPlayerProvider implements ParameterInjector<CommandSender, MatchPlayer> {

  @Override
  public MatchPlayer create(
      CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    CommandSender sender = context.sender();
    if (sender instanceof Player) {
      final MatchPlayer player = PGM.get().getMatchManager().getPlayer((Player) sender);
      if (player != null) return player;
    }

    throw new CommandExecutionException(playerOnly());
  }
}
