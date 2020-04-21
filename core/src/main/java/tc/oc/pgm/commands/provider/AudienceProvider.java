package tc.oc.pgm.commands.provider;

import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.chat.Audience;

public class AudienceProvider implements BukkitProvider<Audience> {

  private final MatchPlayerProvider provider;

  public AudienceProvider(MatchPlayerProvider provider) {
    this.provider = provider;
  }

  @Override
  public boolean isProvided() {
    return true;
  }

  @Override
  public Audience get(CommandSender sender, CommandArgs args, List<? extends Annotation> list) {
    final MatchPlayer player = provider.get(sender, args, list);
    if (player == null) {
      return Audience.get(sender);
    }
    return player;
  }
}
