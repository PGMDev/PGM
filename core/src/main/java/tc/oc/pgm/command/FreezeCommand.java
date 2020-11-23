package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Switch;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.modules.FreezeMatchModule;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.NameStyle;

public class FreezeCommand {

  @Command(
      aliases = {"freeze", "fz", "f"},
      usage = "<player>",
      flags = "s",
      desc = "Toggle a player's frozen state",
      perms = Permissions.FREEZE)
  public void freeze(CommandSender sender, Match match, Player target, @Switch('s') boolean silent)
      throws CommandException {
    setFreeze(sender, match, target, silent);
  }

  @Command(
      aliases = {"frozenlist", "fls", "flist"},
      desc = "View a list of frozen players",
      perms = Permissions.FREEZE)
  public void sendFrozenList(Audience sender, Match match) {
    FreezeMatchModule fmm = match.getModule(FreezeMatchModule.class);

    if (fmm.getFrozenPlayers().isEmpty() && fmm.getOfflineFrozenCount() < 1) {
      sender.sendWarning(TranslatableComponent.of("moderation.freeze.frozenList.none"));
      return;
    }

    // Online Players
    if (!fmm.getFrozenPlayers().isEmpty()) {
      Component names =
          TextComponent.join(
              TextComponent.of(", ", TextColor.GRAY),
              fmm.getFrozenPlayers().stream()
                  .map(m -> m.getName(NameStyle.FANCY))
                  .collect(Collectors.toList()));
      sender.sendMessage(
          formatFrozenList(
              "moderation.freeze.frozenList.online", fmm.getFrozenPlayers().size(), names));
    }

    // Offline Players
    if (fmm.getOfflineFrozenCount() > 0) {
      Component names = TextComponent.of(fmm.getOfflineFrozenNames());
      sender.sendMessage(
          formatFrozenList(
              "moderation.freeze.frozenList.offline", fmm.getOfflineFrozenCount(), names));
    }
  }

  private void setFreeze(CommandSender sender, Match match, Player target, boolean silent) {
    FreezeMatchModule fmm = match.getModule(FreezeMatchModule.class);
    MatchPlayer player = match.getPlayer(target);
    if (player != null) {
      fmm.setFrozen(sender, player, !fmm.isFrozen(player), silent);
    }
  }

  private Component formatFrozenList(String key, int count, Component names) {
    return TranslatableComponent.of(
        key, TextColor.GRAY, TextComponent.of(count, TextColor.AQUA), names);
  }
}
