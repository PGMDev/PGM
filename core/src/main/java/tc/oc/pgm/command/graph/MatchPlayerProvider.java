package tc.oc.pgm.command.graph;

import static tc.oc.pgm.util.text.TextException.exception;

import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public final class MatchPlayerProvider implements BukkitProvider<MatchPlayer> {

  @Override
  public boolean isProvided() {
    return true;
  }

  @Override
  public MatchPlayer get(CommandSender sender, CommandArgs args, List<? extends Annotation> list) {
    if (sender instanceof Player) {
      final MatchPlayer player = PGM.get().getMatchManager().getPlayer((Player) sender);
      if (player != null) {
        return player;
      }

      if (list.stream().anyMatch(annotation -> annotation instanceof Nullable)) {
        return null;
      }
    }

    throw exception("command.onlyPlayers");
  }
}
