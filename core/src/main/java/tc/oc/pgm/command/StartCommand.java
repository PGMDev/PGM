package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TemporalComponent.duration;
import static tc.oc.pgm.util.text.TextException.exception;

import java.time.Duration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.channels.ChatManager;
import tc.oc.pgm.start.StartCountdown;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.start.UnreadyReason;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

public final class StartCommand {

  @Command("start|begin [duration]")
  @CommandDescription("Start the match")
  @Permission(Permissions.START)
  public void start(
      Audience audience,
      CommandSender sender,
      Match match,
      StartMatchModule start,
      @Argument("duration") Duration duration) {
    if (match.isRunning()) {
      throw exception("admin.start.matchRunning");
    } else if (match.isFinished()) {
      throw exception("admin.start.matchFinished");
    }

    if (!start.canStart(true)) {
      audience.sendWarning(translatable("admin.start.unknownState"));
      for (UnreadyReason reason : start.getUnreadyReasons(true)) {
        audience.sendWarning(reason.getReason());
      }
      return;
    }

    match.getCountdown().cancelAll(StartCountdown.class);
    start.forceStartCountdown(duration, null);
    ChatManager.broadcastAdminMessage(translatable(
        "admin.start.announce",
        player(sender, NameStyle.FANCY),
        duration(duration, NamedTextColor.AQUA)));
  }
}
