package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.restart.CancelRestartEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.timelimit.TimeLimitCountdown;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

public final class CancelCommand {

  @CommandMethod("cancel|cancelrestart|cr")
  @CommandDescription("Cancels all countdowns")
  @CommandPermission(Permissions.STOP)
  public void cancel(CommandSender sender, Audience audience, Match match) {
    if (RestartManager.isQueued()) {
      match.callEvent(new CancelRestartEvent());
      audience.sendMessage(translatable("admin.cancelRestart.restartUnqueued", NamedTextColor.RED));
      return;
    }

    if (!match.getCountdown().getAll(TimeLimitCountdown.class).isEmpty()) {
      final TimeLimitMatchModule limits = match.getModule(TimeLimitMatchModule.class);
      limits.cancel();
      limits.setTimeLimit(null);
    }

    match.getCountdown().cancelAll();
    match.needModule(StartMatchModule.class).setAutoStart(false);
    ChatDispatcher.broadcastAdminChatMessage(
        translatable("admin.cancelCountdowns.announce", player(sender, NameStyle.FANCY)), match);
  }
}
