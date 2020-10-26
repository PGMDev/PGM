package tc.oc.pgm.community.command;

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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.Action;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.community.events.PlayerReportEvent;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.chat.Sound;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PeriodFormats;
import tc.oc.pgm.util.text.TextFormatter;

public class ReportCommand {

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
  public void report(MatchPlayer sender, Match match, Player player, @Text String reason)
      throws CommandException {
    if (!sender.getBukkit().hasPermission(Permissions.STAFF)) {
      // Check for cooldown
      Instant lastReport = LAST_REPORT_SENT.getIfPresent(sender.getId());
      if (lastReport != null) {
        Duration timeSinceReport = Duration.between(lastReport, Instant.now());
        long secondsRemaining = REPORT_COOLDOWN_SECONDS - timeSinceReport.getSeconds();
        if (secondsRemaining > 0) {
          TextComponent secondsComponent = Component.text(Long.toString(secondsRemaining));
          TranslatableComponent secondsLeftComponent =
              Component.translatable(
                      secondsRemaining != 1 ? "misc.seconds" : "misc.second", secondsComponent)
                  .color(NamedTextColor.AQUA);
          sender.sendWarning(Component.translatable("command.cooldown", secondsLeftComponent));
          return;
        }
      } else {
        // Player has no cooldown, so add one
        LAST_REPORT_SENT.put(sender.getId(), Instant.now());
      }
    }

    MatchPlayer accused = match.getPlayer(player);

    // Don't allow reports of vanished players
    if (accused.isVanished()) {
      // Due to Intake not accounting for player locale, this message matches what is sent when no
      // player is found.
      // TODO: Please upgrade if command framework uses locale
      sender.sendWarning(
          Component.text(
              "Could not find player named '" + player.getName() + "'", NamedTextColor.RED));
      return;
    }

    PlayerReportEvent event = new PlayerReportEvent(sender, accused, reason);
    match.callEvent(event);

    if (event.isCancelled()) {
      if (event.getCancelMessage() != null) {
        sender.sendMessage(event.getCancelMessage());
      }
      return;
    }

    TranslatableComponent thanks =
        Component.translatable("misc.thankYou", NamedTextColor.GREEN)
            .append(Component.space())
            .append(Component.translatable("moderation.report.acknowledge", NamedTextColor.GOLD));
    sender.sendMessage(thanks);

    final Component component =
        Component.translatable(
            "moderation.report.notify",
            NamedTextColor.YELLOW,
            sender == null ? UsernameFormatUtils.CONSOLE_NAME : sender.getName(NameStyle.FANCY),
            accused.getName(NameStyle.FANCY),
            Component.text(reason.trim(), NamedTextColor.WHITE));

    RECENT_REPORTS.put(
        UUID.randomUUID(),
        new Report(
            accused.getId(),
            sender.getId(),
            accused.getBukkit().getName(),
            sender.getBukkit().getName(),
            reason));

    ChatDispatcher.broadcastAdminChatMessage(component, match, Optional.of(REPORT_NOTIFY_SOUND));
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
      Match match,
      @Default("1") int page,
      @Fallback(Type.NULL) @Switch('t') String target)
      throws CommandException {
    if (RECENT_REPORTS.asMap().isEmpty()) {
      audience.sendMessage(Component.translatable("moderation.reports.none", NamedTextColor.RED));
      return;
    }

    List<Report> reportList = RECENT_REPORTS.asMap().values().stream().collect(Collectors.toList());
    if (target != null) {
      reportList =
          reportList.stream()
              .filter(r -> r.getOfflineTargetName().equalsIgnoreCase(target))
              .collect(Collectors.toList());
    }

    Collections.sort(reportList); // Sort list
    Collections.reverse(reportList); // Reverse so most recent show up first

    Component headerResultCount =
        Component.text(Long.toString(reportList.size()), NamedTextColor.RED);

    int perPage = 6;
    int pages = (reportList.size() + perPage - 1) / perPage;

    Component pageNum =
        Component.translatable(
            "command.simplePageHeader",
            NamedTextColor.AQUA,
            Component.text(Integer.toString(page), NamedTextColor.RED),
            Component.text(Integer.toString(pages), NamedTextColor.RED));

    Component header =
        Component.translatable(
                "moderation.reports.header", NamedTextColor.GRAY, headerResultCount, pageNum)
            .append(
                Component.text(" (")
                    .append(headerResultCount)
                    .append(Component.text(") » "))
                    .append(pageNum));

    Component formattedHeader =
        TextFormatter.horizontalLineHeading(sender, header, NamedTextColor.DARK_GRAY);

    new PrettyPaginatedComponentResults<Report>(formattedHeader, perPage) {
      @Override
      public Component format(Report data, int index) {

        Component reporter =
            Component.translatable(
                "moderation.reports.hover", NamedTextColor.GRAY, data.getSenderComponent(match));

        Component timeAgo =
            PeriodFormats.relativePastApproximate(
                    Instant.ofEpochMilli(data.getTimeSent().toEpochMilli()))
                .color(NamedTextColor.DARK_GREEN);

        return Component.text()
            .append(timeAgo.hoverEvent(HoverEvent.hoverEvent(Action.SHOW_TEXT, reporter)))
            .append(Component.text(": ", NamedTextColor.GRAY))
            .append(data.getTargetComponent(match))
            .append(Component.text(" « ", NamedTextColor.YELLOW))
            .append(Component.text(data.getReason(), NamedTextColor.WHITE, TextDecoration.ITALIC))
            .build();
      }
    }.display(audience, reportList, page);
  }

  private class Report implements Comparable<Report> {
    private final UUID targetUUID;
    private final UUID senderUUID;

    private final String offlineTargetName;
    private final String offlineSenderName;

    private final String reason;
    private final Instant timeSent;

    public Report(UUID target, UUID sender, String targetName, String senderName, String reason) {
      this.targetUUID = target;
      this.senderUUID = sender;
      this.offlineTargetName = targetName;
      this.offlineSenderName = senderName;
      this.reason = reason;
      this.timeSent = Instant.now();
    }

    public UUID getTarget() {
      return targetUUID;
    }

    public UUID getSender() {
      return senderUUID;
    }

    public String getReason() {
      return reason.trim();
    }

    public Instant getTimeSent() {
      return timeSent;
    }

    public String getOfflineTargetName() {
      return offlineTargetName;
    }

    public String getOfflineSenderName() {
      return offlineSenderName;
    }

    public Component getTargetComponent(Match match) {
      return getUsername(getTarget(), match);
    }

    public Component getSenderComponent(Match match) {
      return getUsername(getSender(), match);
    }

    private Component getUsername(UUID uuid, Match match) {
      MatchPlayer player = match.getPlayer(uuid);
      Component name =
          Component.translatable("misc.unknown", NamedTextColor.AQUA, TextDecoration.ITALIC);
      if (match.getPlayer(uuid) != null) {
        name = player.getName(NameStyle.FANCY);
      } else {
        name =
            Component.text(
                uuid.equals(targetUUID) ? getOfflineTargetName() : getOfflineSenderName(),
                NamedTextColor.DARK_AQUA,
                TextDecoration.ITALIC);
      }
      return name;
    }

    @Override
    public int compareTo(Report o) {
      return getTimeSent().compareTo(o.getTimeSent());
    }
  }
}
