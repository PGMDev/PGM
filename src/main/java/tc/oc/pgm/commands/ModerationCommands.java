package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerPunishmentEvent;
import tc.oc.pgm.events.PlayerReportEvent;
import tc.oc.pgm.events.PlayerTimedPunishmentEvent;
import tc.oc.util.components.ComponentUtils;
import tc.oc.util.components.Components;
import tc.oc.util.components.PeriodFormats;

public class ModerationCommands {

  private static final Sound WARN_SOUND = new Sound("mob.enderdragon.growl", 1f, 1f);
  private static final Component WARN_SYMBOL =
      new PersonalizedText(" \u26a0 ").color(ChatColor.YELLOW);

  private static final Component CONSOLE_NAME =
      new PersonalizedTranslatable("console")
          .getPersonalizedText()
          .color(ChatColor.YELLOW)
          .italic(true);

  private static final int REPORT_COOLDOWN_SECONDS = 15;

  private final Cache<UUID, Instant> LAST_REPORT_SENT =
      CacheBuilder.newBuilder().expireAfterWrite(REPORT_COOLDOWN_SECONDS, TimeUnit.SECONDS).build();

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
        java.time.Duration timeSinceReport = java.time.Duration.between(lastReport, Instant.now());
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

  @Command(
      aliases = {"staff", "mods", "admins"},
      desc = "List the online staff members")
  public void staff(CommandSender sender, Match match) {
    // List of online staff
    List<MatchPlayer> onlineStaff =
        match.getPlayers().stream()
            .filter(player -> player.getBukkit().hasPermission(Permissions.STAFF))
            .collect(Collectors.toList());
    // List of formatted names
    List<Component> onlineNames =
        onlineStaff.stream()
            .map(pl -> pl.getStyledName(NameStyle.FANCY))
            .collect(Collectors.toList());

    // FORMAT: Online Staff ({count}): {names}
    Component staffCount =
        new PersonalizedText(Integer.toString(onlineStaff.size()))
            .color(onlineStaff.size() >= 1 ? ChatColor.AQUA : ChatColor.RED);

    BaseComponent content =
        onlineStaff.isEmpty()
            ? new PersonalizedTranslatable("moderation.staff.empty")
                .getPersonalizedText()
                .color(ChatColor.RED)
                .render()
            : Components.join(new PersonalizedText(", ").color(ChatColor.GRAY), onlineNames);

    Component staff =
        new PersonalizedTranslatable("moderation.staff.name", staffCount, content)
            .getPersonalizedText()
            .color(ChatColor.GRAY);

    // Send message
    sender.sendMessage(staff);
  }

  @Command(
      aliases = {"mute", "m"},
      usage = "<player> <reason> -s (silent) -w (warn)",
      desc = "Mute a player",
      perms = Permissions.MUTE)
  public void mute(
      CommandSender sender,
      Player target,
      Match match,
      @Text String reason,
      @Switch('s') boolean silent,
      @Switch('w') boolean warn) {
    MatchPlayer targetMatchPlayer = match.getPlayer(target);

    // if -w, also warn the player but don't broadcast warning
    if (warn) {
      warn(sender, target, match, reason, true);
    }

    // Call Event for general punishments
    punish(PunishmentType.MUTE, targetMatchPlayer, sender, reason, silent);
  }

  @Command(
      aliases = {"warn", "w"},
      usage = "<player> <reason> -s (silent)",
      desc = "Warn a player for bad behavior",
      perms = Permissions.WARN)
  public void warn(
      CommandSender sender,
      Player target,
      Match match,
      @Text String reason,
      @Switch('s') boolean silent) {
    MatchPlayer targetMatchPlayer = match.getPlayer(target);

    // Send warning message to the target
    sendWarning(targetMatchPlayer, reason);

    // Call Event for general punishments
    punish(PunishmentType.WARN, targetMatchPlayer, sender, reason, silent);
  }

  @Command(
      aliases = {"kick", "k"},
      usage = "<player> <reason> -s (silent)",
      desc = "Kick a player from the server",
      perms = Permissions.KICK)
  public void kick(
      CommandSender sender,
      Player target,
      Match match,
      @Text String reason,
      @Switch('s') boolean silent) {

    MatchPlayer targetMatchPlayer = match.getPlayer(target);

    // Kick the player
    target.kickPlayer(
        formatPunishmentScreen(
            PunishmentType.KICK, formatPunisherName(sender, match), reason, null));

    // Call Event for general punishments
    punish(PunishmentType.KICK, targetMatchPlayer, sender, reason, silent);
  }

  @Command(
      aliases = {"ban", "permban", "pb"},
      usage = "<player> <reason> -s (silent)",
      desc = "Ban a player from the server forever",
      perms = Permissions.BAN)
  public void ban(
      CommandSender sender,
      Player target,
      Match match,
      @Text String reason,
      @Switch('s') boolean silent) {
    MatchPlayer targetMatchPlayer = match.getPlayer(target);

    // Ban the player
    // TODO: Implement

    // Kick the now banned player
    target.kickPlayer(
        formatPunishmentScreen(
            PunishmentType.BAN, formatPunisherName(sender, match), reason, null));

    // Call Event for general punishments
    punish(PunishmentType.BAN, targetMatchPlayer, sender, reason, silent);
  }

  @Command(
      aliases = {"tempban", "tban", "tb"},
      usage = "<player> <time> <reason> -s (silent)",
      desc = "Ban a player from the server for a period of time",
      perms = Permissions.BAN)
  public void tempBan(
      CommandSender sender,
      Player target,
      Match match,
      Duration banLength,
      @Text String reason,
      @Switch('s') boolean silent) {
    MatchPlayer targetMatchPlayer = match.getPlayer(target);

    // Kick the now temp-banned player
    target.kickPlayer(
        formatPunishmentScreen(
            PunishmentType.BAN, formatPunisherName(sender, match), reason, banLength));

    // Special event called for TEMP ban
    PlayerTimedPunishmentEvent tempBanEvent =
        new PlayerTimedPunishmentEvent(
            sender, targetMatchPlayer, PunishmentType.TEMP_BAN, reason, silent, banLength);
    match.callEvent(tempBanEvent);
  }

  private void punish(
      PunishmentType type,
      MatchPlayer target,
      CommandSender issuer,
      String reason,
      boolean silent) {
    PlayerPunishmentEvent event = new PlayerPunishmentEvent(issuer, target, type, reason, silent);
    target.getMatch().callEvent(event);
  }

  public static enum PunishmentType {
    MUTE(false),
    WARN(false),
    KICK(true),
    BAN(true),
    TEMP_BAN(false);

    private String PREFIX_TRANSLATE_KEY = "moderation.type.";
    private String SCREEN_TRANSLATE_KEY = "moderation.screen.";

    private final boolean screen;

    PunishmentType(boolean screen) {
      this.screen = screen;
    }

    public Component getPunishmentPrefix() {
      return new PersonalizedTranslatable(PREFIX_TRANSLATE_KEY + name().toLowerCase())
          .getPersonalizedText()
          .color(ChatColor.RED);
    }

    public Component getScreenComponent(Component reason) {
      if (screen) {
        return new PersonalizedTranslatable(SCREEN_TRANSLATE_KEY + name().toLowerCase(), reason)
            .getPersonalizedText()
            .color(ChatColor.GOLD);
      } else {
        return Components.blank();
      }
    }
  }

  /*
   * Format Punisher Name
   */
  public static Component formatPunisherName(CommandSender sender, Match match) {
    Component name = CONSOLE_NAME;
    if (sender instanceof Player) {
      Player senderBukkit = (Player) sender;
      if (senderBukkit != null) {
        MatchPlayer senderMatchPlayer = match.getPlayer(senderBukkit);
        if (senderMatchPlayer != null) {
          name = senderMatchPlayer.getStyledName(NameStyle.FANCY);
        }
      }
    }
    return name;
  }

  /*
   * Format Reason
   */
  public static Component formatPunishmentReason(String reason) {
    return new PersonalizedText(reason).color(ChatColor.RED);
  }

  /*
   * Formatting of Kick Screens (KICK/BAN/TEMPBAN)
   */
  public String formatPunishmentScreen(
      PunishmentType type, Component punisher, String reason, Duration expires) {
    List<Component> lines = Lists.newArrayList();

    Component header =
        new PersonalizedText(
            ComponentUtils.horizontalLineHeading(
                Config.Moderation.getServerName(), ChatColor.DARK_GRAY));

    Component footer =
        new PersonalizedText(
            ComponentUtils.horizontalLine(ChatColor.DARK_GRAY, ComponentUtils.MAX_CHAT_WIDTH));

    Component rules = new PersonalizedText(Config.Moderation.getRulesLink()).color(ChatColor.AQUA);

    lines.add(header); // Header Line (server name) - START
    lines.add(Components.blank());
    lines.add(type.getScreenComponent(formatPunishmentReason(reason))); // The reason
    lines.add(Components.blank());

    // If punishment expires, inform user when
    if (expires != null) {
      Component timeLeft = PeriodFormats.briefNaturalApproximate(expires);
      lines.add(
          new PersonalizedTranslatable("moderation.screen.expires", timeLeft)
              .getPersonalizedText()
              .color(ChatColor.GRAY));
      lines.add(Components.blank());
    }

    // Staff sign-off
    lines.add(
        new PersonalizedTranslatable("moderation.screen.signoff", punisher)
            .getPersonalizedText()
            .color(ChatColor.GRAY)); // The sign-off of who performed the punishment
    lines.add(Components.blank());

    // Link to rules for review by player
    if (Config.Moderation.isRuleLinkVisible()) {
      lines.add(
          new PersonalizedTranslatable("moderation.screen.rulesLink", rules)
              .getPersonalizedText()
              .color(ChatColor.GRAY)); // A link to the rules
    }
    lines.add(Components.blank());

    lines.add(footer); // Footer line - END

    return Components.join(new PersonalizedText("\n" + ChatColor.RESET), lines).toLegacyText();
  }

  /*
   * Sends a formatted title and plays a sound warning a user of their actions
   */
  private void sendWarning(MatchPlayer target, String reason) {
    showWarningTitle(target, reason);
    target.playSound(WARN_SOUND);
  }

  private void showWarningTitle(MatchPlayer target, String reason) {
    Component titleWord =
        new PersonalizedTranslatable("moderation.warning")
            .getPersonalizedText()
            .color(ChatColor.DARK_RED);
    Component title = new PersonalizedText(WARN_SYMBOL, titleWord, WARN_SYMBOL);
    Component subtitle = formatPunishmentReason(reason).color(ChatColor.GOLD);

    target.showTitle(title, subtitle, 5, 200, 10);
  }
}
