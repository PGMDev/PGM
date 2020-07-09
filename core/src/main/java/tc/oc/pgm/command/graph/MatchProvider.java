package tc.oc.pgm.command.graph;

import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.text.TextException;

public final class MatchProvider implements BukkitProvider<Match> {

  @Override
  public boolean isProvided() {
    return true;
  }

  @Override
  public Match get(CommandSender sender, CommandArgs args, List<? extends Annotation> list) {
    final Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match == null) {
      throw TextException.of("command.onlyPlayers");
    }
    return match;
  }
}
