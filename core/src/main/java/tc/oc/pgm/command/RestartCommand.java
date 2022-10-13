package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import java.time.Duration;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.restart.RequestRestartEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.util.Audience;

public final class RestartCommand {

  @CommandMethod("restart|queuerestart|qr [duration]")
  @CommandDescription("Restart the server")
  @CommandPermission(Permissions.STOP)
  public void restart(
      Audience audience,
      Match match,
      @Argument("duration") Duration duration,
      @Flag(value = "force", aliases = "f") boolean force) {
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
