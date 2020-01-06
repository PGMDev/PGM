package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Text;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerReportEvent;

public class ModerationCommands {

  @Command(
      aliases = {"report"},
      usage = "<player> <reason>",
      desc = "Report a player who is breaking the rules")
  public static void report(
      CommandSender commandSender,
      MatchPlayer matchPlayer,
      Match match,
      Player player,
      @Text String reason) {
    MatchPlayer accused = match.getPlayer(player);
    PlayerReportEvent event = new PlayerReportEvent(commandSender, accused, reason);
    match.callEvent(event);

    if (event.isCancelled()) {
      if (event.getCancelMessage() != null) {
        commandSender.sendMessage(event.getCancelMessage());
      }
      return;
    }

    commandSender.sendMessage(
        new PersonalizedText(
            new PersonalizedText(new PersonalizedTranslatable("misc.thankYou"), ChatColor.GREEN),
            new PersonalizedText(" "),
            new PersonalizedText(
                new PersonalizedTranslatable("command.report.acknowledge"), ChatColor.GOLD)));

    final Component component =
        new PersonalizedTranslatable(
            "command.report.notify",
            matchPlayer == null
                ? new PersonalizedText("Console", ChatColor.AQUA, ChatColor.ITALIC)
                : matchPlayer.getStyledName(NameStyle.TAB),
            accused.getStyledName(NameStyle.TAB),
            new PersonalizedText(reason.trim(), ChatColor.WHITE));

    final Component prefixedComponent =
        new PersonalizedText(
            new PersonalizedText("["),
            new PersonalizedText("A", ChatColor.GOLD),
            new PersonalizedText("] "),
            new PersonalizedText(component, ChatColor.YELLOW));

    match.getPlayers().stream()
        .filter(viewer -> viewer.getBukkit().hasPermission(Permissions.ADMINCHAT))
        .forEach(viewer -> viewer.sendMessage(prefixedComponent));
    Audience.get(Bukkit.getConsoleSender()).sendMessage(component);
  }
}
