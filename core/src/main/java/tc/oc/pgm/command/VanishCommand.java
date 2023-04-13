package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;

public class VanishCommand {

  private static final Component ACTIVATE = translatable("vanish.activate", NamedTextColor.GREEN);
  private static final Component DEACTIVATE =
      translatable("vanish.deactivate", NamedTextColor.GREEN);

  @CommandMethod("vanish|v")
  @CommandDescription("Toggle vanish status")
  @CommandPermission(Permissions.VANISH)
  public void vanish(MatchPlayer sender, @Flag(value = "silent", aliases = "s") boolean silent) {
    boolean isVanished = Integration.isVanished(sender.getBukkit());
    boolean result = Integration.setVanished(sender, !isVanished, silent);
    sender.sendWarning(result ? ACTIVATE : DEACTIVATE);
  }
}
