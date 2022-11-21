package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;

public class VanishCommand {

  @CommandMethod("vanish|v")
  @CommandDescription("Toggle vanish status")
  @CommandPermission(Permissions.VANISH)
  public void vanish(MatchPlayer sender, @Flag(value = "silent", aliases = "s") boolean silent) {
    if (Integration.getNick(sender.getBukkit()) != null) {
      sender.sendWarning(translatable("vanish.deny.nick"));
      return;
    }

    if (Integration.setVanished(sender, !Integration.isVanished(sender.getBukkit()), silent)) {
      sender.sendWarning(translatable("vanish.activate").color(NamedTextColor.GREEN));
    } else {
      sender.sendWarning(translatable("vanish.deactivate").color(NamedTextColor.RED));
    }
  }
}
