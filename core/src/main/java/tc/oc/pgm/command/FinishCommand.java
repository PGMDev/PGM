package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TextException.exception;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.channels.ChatManager;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.named.NameStyle;

public final class FinishCommand {

  @Command("finish|end [team]")
  @CommandDescription("End the match")
  @Permission(Permissions.STOP)
  public void end(CommandSender sender, Match match, @Argument("team") Team team) {
    if (!match.finish(team)) {
      throw exception("admin.end.unknownError");
    } else {
      ChatManager.broadcastAdminMessage(
          translatable("admin.end.announce", player(sender, NameStyle.FANCY)));
    }
  }
}
