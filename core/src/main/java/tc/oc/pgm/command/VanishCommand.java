package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Switch;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.VanishManager;
import tc.oc.pgm.nick.NickRegistry;

public class VanishCommand {

  /* Commands */
  @Command(
      aliases = {"vanish", "v"},
      desc = "Toggle vanish status",
      perms = Permissions.VANISH)
  public void vanish(MatchPlayer sender, @Switch('s') boolean silent) throws CommandException {
    NickRegistry nicks = PGM.get().getNickRegistry();
    VanishManager vanish = PGM.get().getVanishManager();

    if (nicks.getNick(sender.getId()).isPresent()) {
      sender.sendWarning(translatable("vanish.deny.nick"));
      return;
    }

    if (vanish.setVanished(sender, !vanish.isVanished(sender.getId()), silent)) {
      sender.sendWarning(translatable("vanish.activate").color(NamedTextColor.GREEN));
    } else {
      sender.sendWarning(translatable("vanish.deactivate").color(NamedTextColor.RED));
    }
  }
}
