package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Text;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerReportEvent;

public class ModerationCommands {

  private static final int REPORT_COOLDOWN_SECONDS = 15;

  private static final LoadingCache<UUID, Instant> lastReportSent =
      CacheBuilder.newBuilder()
          .weakKeys()
          .expireAfterWrite(REPORT_COOLDOWN_SECONDS, TimeUnit.SECONDS)
          .build(
              new CacheLoader<UUID, Instant>() {
                @Override
                public Instant load(UUID uuid) throws Exception {
                  return Instant.now();
                }
              });

  @Command(
      aliases = {"report"},
      usage = "<player> <reason>",
      desc = "Report a player who is breaking the rules")
  public static void report(
      CommandSender commandSender,
      MatchPlayer matchPlayer,
      Match match,
      Player player,
      @Text String reason)
      throws CommandException {
    if (!commandSender.hasPermission(Permissions.STAFF) && commandSender instanceof Player) {
      // Check for cooldown
      Instant lastReport = lastReportSent.getIfPresent(matchPlayer.getId());
      if (lastReport != null) {
        Duration timeSinceReport = Duration.between(lastReport, Instant.now());
        long secondsRemaining = REPORT_COOLDOWN_SECONDS - timeSinceReport.getSeconds();
        if (secondsRemaining > 0) {
          throw new CommandException(
              AllTranslations.get()
                  .translate(
                      "command.report.cooldown",
                      commandSender,
                      ChatColor.AQUA
                          + (secondsRemaining
                              + " second"
                              + (secondsRemaining != 1 ? "s" : "")
                              + ChatColor.RED)));
        }
      } else {
        // Player has no cooldown, so add one
        lastReportSent.put(matchPlayer.getId(), Instant.now());
      }
    }

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
                : matchPlayer.getStyledName(NameStyle.FANCY),
            accused.getStyledName(NameStyle.FANCY),
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
