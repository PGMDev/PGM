package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import java.time.Duration;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.start.StartCountdown;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.start.UnreadyReason;
import tc.oc.pgm.util.Audience;

public final class StartCommand {

  @CommandMethod("start|begin [duration]")
  @CommandDescription("Start the match")
  @CommandPermission(Permissions.START)
  public void start(
      Audience audience,
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
  }
}
