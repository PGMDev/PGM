package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;

public class VanishCommand {

  private static final Component ACTIVATE = translatable("vanish.activate", NamedTextColor.GREEN);
  private static final Component DEACTIVATE =
      translatable("vanish.deactivate", NamedTextColor.GREEN);

  @Command("vanish|v")
  @CommandDescription("Toggle vanish status")
  @Permission(Permissions.VANISH)
  public void vanish(MatchPlayer sender, @Flag(value = "silent", aliases = "s") boolean silent) {
    boolean isVanished = Integration.isVanished(sender.getBukkit());
    boolean result = Integration.setVanished(sender, !isVanished, silent);
    sender.sendWarning(result ? ACTIVATE : DEACTIVATE);
  }
}
