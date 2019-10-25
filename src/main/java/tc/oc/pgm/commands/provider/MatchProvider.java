package tc.oc.pgm.commands.provider;

import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchManager;

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
  public Match get(CommandSender sender, CommandArgs args, List<? extends Annotation> list) {
    return matchManager.getCurrentMatch(sender);
  }
}
