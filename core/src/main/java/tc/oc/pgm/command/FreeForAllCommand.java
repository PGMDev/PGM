package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import com.google.common.collect.Range;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.channels.ChatManager;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextParser;

@Command("ffa|players")
public final class FreeForAllCommand {

  @Command("min <min-players>")
  @CommandDescription("Set the min players")
  @Permission(Permissions.RESIZE)
  public void min(
      Match match,
      CommandSender sender,
      FreeForAllMatchModule ffa,
      @Argument("min-players") int minPlayers) {
    TextParser.assertInRange(minPlayers, Range.atLeast(0));

    ffa.setMinPlayers(minPlayers);
    sendResizedMessage(match, sender, "min", ffa.getMinPlayers());
  }

  @Command("min reset")
  @CommandDescription("Reset the min players")
  @Permission(Permissions.RESIZE)
  public void min(Match match, CommandSender sender, FreeForAllMatchModule ffa) {
    ffa.setMinPlayers(null);
    sendResizedMessage(match, sender, "min", ffa.getMinPlayers());
  }

  @Command("scale <factor>")
  @CommandDescription("Scale the max players by a given factor")
  @Permission(Permissions.RESIZE)
  public void max(
      Match match,
      CommandSender sender,
      FreeForAllMatchModule ffa,
      @Argument("factor") double scale) {
    int maxOverfill = (int) (ffa.getMaxOverfill() * scale);
    int maxSize = (int) (ffa.getMaxPlayers() * scale);
    ffa.setMaxPlayers(maxSize, maxOverfill);

    sendResizedMessage(match, sender, "max", ffa.getMaxPlayers());
  }

  @Command("max <max-players> [max-overfill]")
  @CommandDescription("Set the max players")
  @Permission(Permissions.RESIZE)
  public void max(
      Match match,
      CommandSender sender,
      FreeForAllMatchModule ffa,
      @Argument("max-players") int maxPlayers,
      @Argument("max-overfill") Integer maxOverfill) {
    TextParser.assertInRange(maxPlayers, Range.atLeast(ffa.getMinPlayers()));

    if (maxOverfill == null) maxOverfill = (int) Math.ceil(1.25 * maxPlayers);
    else TextParser.assertInRange(maxOverfill, Range.atLeast(maxPlayers));

    ffa.setMaxPlayers(maxPlayers, maxOverfill);

    sendResizedMessage(match, sender, "max", ffa.getMaxPlayers());
  }

  @Command("max reset")
  @CommandDescription("Reset the max players")
  @Permission(Permissions.RESIZE)
  public void max(Match match, CommandSender sender, FreeForAllMatchModule ffa) {
    ffa.setMaxPlayers(null, null);
    sendResizedMessage(match, sender, "max", ffa.getMaxPlayers());
  }

  private void sendResizedMessage(Match match, CommandSender sender, String type, int value) {
    ChatManager.broadcastAdminMessage(translatable(
        "match.resize.announce." + type,
        player(sender, NameStyle.FANCY),
        translatable("match.info.players", NamedTextColor.YELLOW),
        text(value, NamedTextColor.AQUA)));
  }
}
