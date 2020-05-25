package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import java.time.Duration;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.restart.RequestRestartEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.util.chat.Audience;

public final class RestartCommand {

  @Command(
      aliases = {"restart", "queuerestart", "qr"},
      desc = "Restart the server",
      flags = "f",
      perms = Permissions.STOP)
  public void restart(
      Audience audience,
      Match match,
      @Default("30s") Duration duration,
      @Switch('f') boolean force) {
    RestartManager.queueRestart("Restart requested via /queuerestart command", duration);

    if (force && match.isRunning()) {
      match.finish();
    }

    if (match.isRunning()) {
      audience.sendMessage(
          TranslatableComponent.of("admin.queueRestart.restartQueued", TextColor.RED));
    } else {
      audience.sendMessage(
          TranslatableComponent.of("admin.queueRestart.restartingNow", TextColor.GREEN));
    }

    match.callEvent(new RequestRestartEvent());
  }
}
