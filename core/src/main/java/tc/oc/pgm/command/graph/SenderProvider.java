package tc.oc.pgm.command.graph;

import static tc.oc.pgm.util.text.TextException.exception;

import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class SenderProvider implements BukkitProvider<Sender> {

  @Override
  public boolean isProvided() {
    return true;
  }

  @Override
  public Sender get(CommandSender sender, CommandArgs args, List<? extends Annotation> list) {
    Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match == null) throw exception("command.onlyPlayers");

    return new Sender(sender, match, null);
  }

  public static class PlayerSenderProvider implements BukkitProvider<Sender.Player> {

    @Override
    public boolean isProvided() {
      return true;
    }

    @Override
    public Sender.Player get(
        CommandSender sender, CommandArgs args, List<? extends Annotation> list) {
      if (sender instanceof Player) {
        MatchPlayer player = PGM.get().getMatchManager().getPlayer((Player) sender);
        if (player != null) return new Sender.Player((Player) sender, player.getMatch(), player);
      }

      throw exception("command.onlyPlayers");
    }
  }
}
