package tc.oc.pgm.commands.provider;

import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;

public class MatchProvider implements BukkitProvider<Match> {

  private final MatchManager matchManager;

  public MatchProvider(MatchManager matchManager) {
    this.matchManager = matchManager;
  }

  @Override
  public boolean isProvided() {
    return true;
  }

  @Override
  public Match get(CommandSender sender, CommandArgs args, List<? extends Annotation> list)
      throws ArgumentException {
    final Match match = matchManager.getMatch(sender);
    if (match == null) {
      throw new ArgumentException("You must be in a Match to use this command!");
    }
    return match;
  }
}
