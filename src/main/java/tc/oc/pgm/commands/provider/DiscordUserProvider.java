package tc.oc.pgm.commands.provider;

import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import app.ashcon.intake.parametric.ProvisionException;
import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.discord.DiscordId;

public class DiscordUserProvider implements BukkitProvider<DiscordId> {

  @Override
  public boolean isProvided() {
    return true;
  }

  @Override
  public DiscordId get(
      CommandSender sender, CommandArgs args, List<? extends Annotation> annotations)
      throws ArgumentException, ProvisionException {
    if (sender instanceof Player) {
      return PGM.get().getDatastore().getDiscordId(((Player) sender).getUniqueId());
    }
    throw new ArgumentException(AllTranslations.get().translate("command.onlyPlayers", sender));
  }
}
