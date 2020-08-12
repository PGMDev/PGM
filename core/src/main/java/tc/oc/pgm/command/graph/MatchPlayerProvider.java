package tc.oc.pgm.command.graph;

import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.text.TextException;

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

    throw TextException.of("command.onlyPlayers");
  }
}
