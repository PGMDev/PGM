package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import com.google.common.collect.Range;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.text.TextParser;

@CommandMethod("ffa|players")
public final class FreeForAllCommand {

  @CommandMethod("min <min-players>")
  @CommandDescription("Set the min players")
  @CommandPermission(Permissions.RESIZE)
  public void min(
      Audience audience, FreeForAllMatchModule ffa, @Argument("min-players") int minPlayers) {
    TextParser.assertInRange(minPlayers, Range.atLeast(0));

    ffa.setMinPlayers(minPlayers);
    sendResizedMessage(audience, "min", ffa.getMinPlayers());
  }

  @CommandMethod("min reset")
  @CommandDescription("Reset the min players")
  @CommandPermission(Permissions.RESIZE)
  public void min(Audience audience, FreeForAllMatchModule ffa) {
    ffa.setMinPlayers(null);
    sendResizedMessage(audience, "min", ffa.getMinPlayers());
  }

  @CommandMethod("max <max-players> [max-overfill]")
  @CommandDescription("Set the max players")
  @CommandPermission(Permissions.RESIZE)
  public void max(
      Audience audience,
      FreeForAllMatchModule ffa,
      @Argument("max-players") int maxPlayers,
      @Argument("max-overfill") Integer maxOverfill) {
    TextParser.assertInRange(maxPlayers, Range.atLeast(ffa.getMinPlayers()));

    if (maxOverfill == null) maxOverfill = (int) Math.ceil(1.25 * maxPlayers);
    else TextParser.assertInRange(maxOverfill, Range.atLeast(maxPlayers));

    ffa.setMaxPlayers(maxPlayers, maxOverfill);

    sendResizedMessage(audience, "max", ffa.getMaxPlayers());
  }

  @CommandMethod("max reset")
  @CommandDescription("Reset the max players")
  @CommandPermission(Permissions.RESIZE)
  public void max(Audience audience, FreeForAllMatchModule ffa) {
    ffa.setMaxPlayers(null, null);
    sendResizedMessage(audience, "max", ffa.getMaxPlayers());
  }

  private void sendResizedMessage(Audience audience, String type, int value) {
    audience.sendMessage(
        translatable(
            "match.resize." + type,
            translatable("match.info.players", NamedTextColor.YELLOW),
            text(value, NamedTextColor.AQUA)));
  }
}
