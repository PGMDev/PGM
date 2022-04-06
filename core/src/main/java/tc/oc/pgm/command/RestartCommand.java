package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Switch;
import java.time.Duration;
import javax.annotation.Nullable;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.restart.RequestRestartEvent;
import tc.oc.pgm.restart.RestartManager;

public final class RestartCommand {

  @Command(
      aliases = {"restart", "queuerestart", "qr"},
      desc = "Restart the server",
      flags = "f",
      perms = Permissions.STOP)
  public void restart(
      Audience audience, Match match, @Nullable Duration duration, @Switch('f') boolean force) {
    RestartManager.queueRestart("Restart requested via /queuerestart command", duration);

    if (force && match.isRunning()) {
      match.finish();
    }

    if (match.isRunning()) {
      audience.sendMessage(translatable("admin.queueRestart.restartQueued", NamedTextColor.RED));
    } else {
      audience.sendMessage(translatable("admin.queueRestart.restartingNow", NamedTextColor.GREEN));
    }

    match.callEvent(new RequestRestartEvent());
  }
}
