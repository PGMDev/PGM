package tc.oc.pgm.community.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerReportEvent;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.chat.Sound;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.component.ComponentUtils;
import tc.oc.pgm.util.component.PeriodFormats;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.named.NameStyle;

public class ReportCommands {

  private static final Sound REPORT_NOTIFY_SOUND = new Sound("random.pop", 1f, 1.2f);

  private static final int REPORT_COOLDOWN_SECONDS = 15;
  private static final int REPORT_EXPIRE_HOURS = 1;

  private final Cache<UUID, Instant> LAST_REPORT_SENT =
      CacheBuilder.newBuilder().expireAfterWrite(REPORT_COOLDOWN_SECONDS, TimeUnit.SECONDS).build();

  private final Cache<UUID, Report> RECENT_REPORTS =
      CacheBuilder.newBuilder().expireAfterWrite(REPORT_EXPIRE_HOURS, TimeUnit.HOURS).build();

  @Command(
      aliases = {"report"},
      usage = "<player> <reason>",
      desc = "Report a player who is breaking the rules")
  public void report(
      CommandSender commandSender,
      MatchPlayer matchPlayer,
      Match match,
      Player player,
      @Text String reason)
      throws CommandException {
    if (!commandSender.hasPermission(Permissions.STAFF) && commandSender instanceof Player) {
      // Check for cooldown
      Instant lastReport = LAST_REPORT_SENT.getIfPresent(matchPlayer.getId());
      if (lastReport != null) {
        Duration timeSinceReport = Duration.between(lastReport, Instant.now());
        long secondsRemaining = REPORT_COOLDOWN_SECONDS - timeSinceReport.getSeconds();
        if (secondsRemaining > 0) {
          Component secondsComponent = new PersonalizedText(Long.toString(secondsRemaining));
          Component secondsLeftComponent =
              new PersonalizedTranslatable(
                      secondsRemaining != 1
                          ? "misc.time.seconds"
                          : "misc.time.second",
                      secondsComponent)
                  .getPersonalizedText()
                  .color(ChatColor.AQUA);
          commandSender.sendMessage(
              new PersonalizedTranslatable("command.cooldown", secondsLeftComponent)
                  .getPersonalizedText()
                  .color(ChatColor.RED));
          return;
        }
      } else {
        // Player has no cooldown, so add one
        LAST_REPORT_SENT.put(matchPlayer.getId(), Instant.now());
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
                new PersonalizedTranslatable("moderation.report.acknowledge"), ChatColor.GOLD)));

    final Component component =
        new PersonalizedTranslatable(
                "moderation.report.notify",
            matchPlayer == null
                ? new PersonalizedText("Console", ChatColor.AQUA, ChatColor.ITALIC)
                : matchPlayer.getStyledName(NameStyle.FANCY),
            accused.getStyledName(NameStyle.FANCY),
            new PersonalizedText(reason.trim(), ChatColor.WHITE));

    RECENT_REPORTS.put(
        UUID.randomUUID(),
        new Report(
            player.getName(),
            reason,
            accused.getStyledName(NameStyle.FANCY),
            matchPlayer.getStyledName(NameStyle.CONCISE)));

    ChatDispatcher.broadcastAdminChatMessage(
        new PersonalizedText(component, ChatColor.YELLOW), match, Optional.of(REPORT_NOTIFY_SOUND));
  }

  @Command(
      aliases = {"reports", "reps", "reporthistory"},
      desc = "Display a list of recent reports",
      usage = "(page) -t [target player]",
      flags = "t",
      perms = Permissions.STAFF)
  public void reportHistory(
      Audience audience,
      CommandSender sender,
      @Default("1") int page,
      @Fallback(Type.NULL) @Switch('t') String target)
      throws CommandException {
    if (RECENT_REPORTS.asMap().isEmpty()) {
      sender.sendMessage(
          new PersonalizedTranslatable("moderation.reports.none")
              .getPersonalizedText()
              .color(ChatColor.RED));
      return;
    }

    List<Report> reportList = RECENT_REPORTS.asMap().values().stream().collect(Collectors.toList());
    if (target != null) {
      reportList =
          reportList.stream()
              .filter(r -> r.getId().equalsIgnoreCase(target))
              .collect(Collectors.toList());
    }

    Collections.sort(reportList); // Sort list
    Collections.reverse(reportList); // Reverse so most recent show up first

    Component headerResultCount =
        new PersonalizedText(Long.toString(reportList.size())).color(ChatColor.RED);

    int perPage = 6;
    int pages = (reportList.size() + perPage - 1) / perPage;

    Component pageNum =
        new PersonalizedTranslatable(
                "command.simplePageHeader",
                new PersonalizedText(Integer.toString(page)).color(ChatColor.RED),
                new PersonalizedText(Integer.toString(pages)).color(ChatColor.RED))
            .add(ChatColor.AQUA);

    Component header =
        new PersonalizedText(
            new PersonalizedTranslatable("moderation.reports.header", headerResultCount, pageNum)
                .getPersonalizedText()
                .color(ChatColor.GRAY),
            new PersonalizedText(" ("),
            headerResultCount,
            new PersonalizedText(") » "),
            pageNum);

    Component formattedHeader =
        new PersonalizedText(
            ComponentUtils.horizontalLineHeading(
                ComponentRenderers.toLegacyText(header, sender), ChatColor.DARK_GRAY));

    new PrettyPaginatedComponentResults<Report>(formattedHeader, perPage) {
      @Override
      public Component format(Report data, int index) {

        Component reporter =
            new PersonalizedTranslatable("moderation.reports.hover", data.getSenderName())
                .color(ChatColor.GRAY);

        Component timeAgo =
            PeriodFormats.relativePastApproximate(
                    java.time.Instant.ofEpochMilli(data.getTimeSent().toEpochMilli()))
                .color(ChatColor.DARK_AQUA);

        return new PersonalizedText(
            new PersonalizedText(timeAgo.render(sender))
                .hoverEvent(Action.SHOW_TEXT, reporter.render(sender)),
            new PersonalizedText(": ").color(ChatColor.GRAY),
            data.getTargetName(),
            new PersonalizedText(" « ").color(ChatColor.YELLOW),
            new PersonalizedText(data.getReason()).italic(true).color(ChatColor.WHITE));
      }
    }.display(audience, reportList, page);
  }

  public static class Report implements Comparable<Report> {
    private final String id;
    private final String reason;
    private final Component targetName;
    private final Component sender;
    private final Instant timeSent;

    public Report(String id, String reason, Component targetName, Component sender) {
      this.id = id;
      this.reason = reason;
      this.targetName = targetName;
      this.sender = sender;
      this.timeSent = Instant.now();
    }

    public String getId() {
      return id;
    }

    public String getReason() {
      return reason;
    }

    public Component getTargetName() {
      return targetName;
    }

    public Component getSenderName() {
      return sender;
    }

    public Instant getTimeSent() {
      return timeSent;
    }

    @Override
    public int compareTo(Report o) {
      return getTimeSent().compareTo(o.getTimeSent());
    }
  }
}
