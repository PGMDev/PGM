package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.restart.CancelRestartEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.timelimit.TimeLimitCountdown;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.chat.Audience;

public final class CancelCommand {

  @Command(
      aliases = {"cancel", "cancelrestart", "cr"},
      desc = "Cancels all countdowns",
      perms = Permissions.STOP)
  public void cancel(Audience audience, CommandSender sender, Match match) {
    if (RestartManager.isQueued()) {
      match.callEvent(new CancelRestartEvent());
      audience.sendMessage(
          TranslatableComponent.of("admin.cancelRestart.restartUnqueued", TextColor.RED));
      ChatDispatcher.broadcastAdminChatMessage(
          TranslatableComponent.of("admin.cancelRestart.broadcast", TextColor.GRAY)
              .args(UsernameFormatUtils.formatStaffName(sender, match)),
          match);
      return;
    }

    if (!match.getCountdown().getAll(TimeLimitCountdown.class).isEmpty()) {
      final TimeLimitMatchModule limits = match.getModule(TimeLimitMatchModule.class);
      limits.cancel();
      limits.setTimeLimit(null);
    }

    match.getCountdown().cancelAll();
    match.needModule(StartMatchModule.class).setAutoStart(false);
    audience.sendMessage(TranslatableComponent.of("admin.cancelCountdowns", TextColor.GREEN));
  }
}
