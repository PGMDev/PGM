package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;

import app.ashcon.intake.Command;
import java.time.Duration;
import javax.annotation.Nullable;
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.start.StartCountdown;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.start.UnreadyReason;

public final class StartCommand {

  @Command(
      aliases = {"start", "begin"},
      desc = "Start the match",
      perms = Permissions.START)
  public void start(Audience audience, Match match, @Nullable Duration duration) {
    if (match.isRunning()) {
      throw exception("admin.start.matchRunning");
    } else if (match.isFinished()) {
      throw exception("admin.start.matchFinished");
    }

    final StartMatchModule start = match.needModule(StartMatchModule.class);
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
