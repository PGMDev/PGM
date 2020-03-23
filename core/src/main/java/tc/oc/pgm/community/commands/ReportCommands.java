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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.events.PlayerReportEvent;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.util.bukkit.chat.Audience;
import tc.oc.util.bukkit.chat.Sound;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.ComponentRenderers;
import tc.oc.util.bukkit.component.ComponentUtils;
import tc.oc.util.bukkit.component.PeriodFormats;
import tc.oc.util.bukkit.component.types.PersonalizedText;
import tc.oc.util.bukkit.component.types.PersonalizedTranslatable;
import tc.oc.util.bukkit.named.NameStyle;

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
                          ? "countdown.pluralCompound"
                          : "countdown.singularCompound",
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
                new PersonalizedTranslatable("command.report.acknowledge"), ChatColor.GOLD)));

    final Component component =
        new PersonalizedTranslatable(
            "command.report.notify",
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
            accused.getStyledName(NameStyle.CONCISE),
            matchPlayer.getStyledName(NameStyle.CONCISE)));

    final Component prefixedComponent =
        new PersonalizedText(
            new PersonalizedText(ChatDispatcher.ADMIN_CHAT_PREFIX),
            new PersonalizedText(component, ChatColor.YELLOW));

    match.getPlayers().stream()
        .filter(viewer -> viewer.getBukkit().hasPermission(Permissions.ADMINCHAT))
        .forEach(
            viewer -> {
              // Play sound for viewers of reports
              if (viewer
                  .getSettings()
                  .getValue(SettingKey.SOUNDS)
                  .equals(SettingValue.SOUNDS_ALL)) {
                viewer.playSound(REPORT_NOTIFY_SOUND);
              }
              viewer.sendMessage(prefixedComponent);
            });
    Audience.get(Bukkit.getConsoleSender()).sendMessage(component);
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
                "command.paginatedResult.page",
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
        Component timeAgo =
            PeriodFormats.relativePastApproximate(
                    org.joda.time.Instant.ofEpochMilli(data.getTimeSent().toEpochMilli()))
                .color(ChatColor.DARK_AQUA);
        Component hover =
            new PersonalizedTranslatable("moderation.reports.hover", data.getSenderName())
                .getPersonalizedText()
                .color(ChatColor.GRAY);

        Component formatted =
            new PersonalizedText(
                timeAgo,
                new PersonalizedText(": ").color(ChatColor.GRAY),
                data.getTargetName(),
                new PersonalizedText(" « ").color(ChatColor.YELLOW),
                new PersonalizedText(data.getReason()).italic(true).color(ChatColor.WHITE));

        return formatted.hoverEvent(Action.SHOW_TEXT, hover.render(sender));
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
