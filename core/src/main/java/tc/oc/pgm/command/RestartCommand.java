package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import java.time.Duration;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.restart.RequestRestartEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.chat.Audience;

public final class RestartCommand {

  @Command(
      aliases = {"restart", "queuerestart", "qr"},
      desc = "Restart the server",
      flags = "f",
      perms = Permissions.STOP)
  public void restart(
      Audience audience,
      CommandSender sender,
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
      ChatDispatcher.broadcastAdminChatMessage(
          TranslatableComponent.of("admin.queueRestart.broadcast", TextColor.GRAY)
              .args(UsernameFormatUtils.formatStaffName(sender, match)),
          match);
    } else {
      audience.sendMessage(
          TranslatableComponent.of("admin.queueRestart.restartingNow", TextColor.GREEN));
    }

    match.callEvent(new RequestRestartEvent());
  }
}
