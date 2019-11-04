package tc.oc.pgm.commands.provider;

import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;

public class MatchPlayerProvider implements BukkitProvider<MatchPlayer> {

  private final MatchManager matchManager;

  public MatchPlayerProvider(MatchManager matchManager) {
    this.matchManager = matchManager;
  }

  @Override
  public boolean isProvided() {
    return true;
  }

  @Override
  public MatchPlayer get(CommandSender sender, CommandArgs args, List<? extends Annotation> list) {
    if (sender instanceof Entity) {
      return matchManager.getPlayer((Entity) sender);
    }
    return null;
  }
}
