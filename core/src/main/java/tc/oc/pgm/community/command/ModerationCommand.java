package tc.oc.pgm.community.command;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.title.Title.title;
import static tc.oc.pgm.util.TimeUtils.fromTicks;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import tc.oc.pgm.PGMConfig;
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.named.NameStyle;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.player.VanishManager;
import tc.oc.pgm.community.events.PlayerPunishmentEvent;
import tc.oc.pgm.community.modules.FreezeMatchModule;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;
import tc.oc.pgm.util.xml.XMLUtils;

public class ModerationCommand implements Listener {

  private static final Sound WARN_SOUND =
      sound(key("mob.enderdragon.growl"), Sound.Source.MASTER, 1f, 1f);
  private static final Sound ALT_SOUND =
      sound(key("mob.wither.shoot"), Sound.Source.MASTER, 0.8f, 1.3f);

  private static final Component WARN_SYMBOL = text(" \u26a0 ", NamedTextColor.YELLOW);
  private static final Component BROADCAST_DIV = text(" \u00BB ", NamedTextColor.GRAY);

  private final ChatDispatcher chat;
  private final MatchManager manager;
  private final VanishManager vanish;

  private final List<BannedAccountInfo> recentBans;

  public ModerationCommand() {
    this.chat = ChatDispatcher.get();
    this.manager = PGM.get().getMatchManager();
    this.vanish = PGM.get().getVanishManager();
    this.recentBans = Lists.newArrayList();
    PGM.get().getServer().getPluginManager().registerEvents(this, PGM.get());
  }

  private void cacheRecentBan(Player banned, Component punisher) {
    this.recentBans.add(new BannedAccountInfo(banned, punisher));
  }

  private void removeCachedBan(BannedAccountInfo info) {
    recentBans.remove(info);
  }

  private Optional<BannedAccountInfo> getBanWithMatchingIP(String address) {
    return recentBans.stream().filter(b -> b.isSameAddress(address)).findFirst();
  }

  private Optional<BannedAccountInfo> getBanWithMatchingName(String name) {
    return recentBans.stream().filter(b -> b.getUserName().equalsIgnoreCase(name)).findFirst();
  }

  @Command(
      aliases = {"staff", "mods", "admins"},
      desc = "List the online staff members")
  public void staff(Audience viewer, CommandSender sender, Match match) {
    // List of online staff based off of permission
    List<Component> onlineStaff =
        match.getPlayers().stream()
            .filter(
                player ->
                    (player.getBukkit().hasPermission(Permissions.STAFF)
                        && (!player.isVanished() || sender.hasPermission(Permissions.STAFF))))
            .map(p -> p.getName(NameStyle.VERBOSE))
            .collect(Collectors.toList());

    // FORMAT: Online Staff ({count}): {names}
    Component staffCount =
        text(onlineStaff.size())
            .color(onlineStaff.isEmpty() ? NamedTextColor.RED : NamedTextColor.AQUA);

    Component content =
        onlineStaff.isEmpty()
            ? translatable("moderation.staff.empty")
            : TextFormatter.list(onlineStaff, NamedTextColor.GRAY);

    Component staff =
        translatable("moderation.staff.name", NamedTextColor.GRAY, staffCount, content);

    // Send message
    viewer.sendMessage(staff);
  }

  @Command(
      aliases = {"frozenlist", "fls", "flist"},
      desc = "View a list of frozen players",
      perms = Permissions.FREEZE)
  public void sendFrozenList(Audience sender, Match match) {
    FreezeMatchModule fmm = match.getModule(FreezeMatchModule.class);

    if (fmm.getFrozenPlayers().isEmpty() && fmm.getOfflineFrozenCount() < 1) {
      sender.sendWarning(translatable("moderation.freeze.frozenList.none"));
      return;
    }

    // Online Players
    if (!fmm.getFrozenPlayers().isEmpty()) {
      Component names =
          join(
              text(", ", NamedTextColor.GRAY),
              fmm.getFrozenPlayers().stream()
                  .map(m -> m.getName(NameStyle.FANCY))
                  .collect(Collectors.toList()));
      sender.sendMessage(
          formatFrozenList(
              "moderation.freeze.frozenList.online", fmm.getFrozenPlayers().size(), names));
    }

    // Offline Players
    if (fmm.getOfflineFrozenCount() > 0) {
      Component names = fmm.getOfflineFrozenNames();
      sender.sendMessage(
          formatFrozenList(
              "moderation.freeze.frozenList.offline", fmm.getOfflineFrozenCount(), names));
    }
  }

  private Component formatFrozenList(String key, int count, Component names) {
    return translatable(key, NamedTextColor.GRAY, text(count, NamedTextColor.AQUA), names);
  }

  @Command(
      aliases = {"freeze", "fz", "f"},
      usage = "<player>",
      flags = "s",
      desc = "Toggle a player's frozen state",
      perms = Permissions.FREEZE)
  public void freeze(CommandSender sender, Match match, Player target, @Switch('s') boolean silent)
      throws CommandException {
    setFreeze(sender, match, target, checkSilent(silent, sender));
  }

  private void setFreeze(CommandSender sender, Match match, Player target, boolean silent) {
    FreezeMatchModule fmm = match.getModule(FreezeMatchModule.class);
    MatchPlayer player = match.getPlayer(target);
    if (player != null) {
      fmm.setFrozen(sender, player, !fmm.isFrozen(player), silent);
    }
  }

  @Command(
      aliases = {"mute", "m"},
      usage = "<player> <reason>",
      desc = "Mute a player",
      perms = Permissions.MUTE)
  public void mute(
      Audience viewer, CommandSender sender, Player target, Match match, @Text String reason) {
    MatchPlayer targetMatchPlayer = match.getPlayer(target);
    if (chat.isMuted(targetMatchPlayer)) {
      viewer.sendWarning(
          translatable("moderation.mute.existing", targetMatchPlayer.getName(NameStyle.FANCY)));
      return;
    }

    // Send a warning to the player to identify mute reason
    warn(sender, target, match, reason);

    if (punish(PunishmentType.MUTE, targetMatchPlayer, sender, reason, true)) {
      chat.addMuted(targetMatchPlayer, reason);
    }
  }

  @Command(
      aliases = {"mutes", "mutelist"},
      desc = "List of muted players",
      perms = Permissions.MUTE)
  public void listMutes(Audience viewer, CommandSender sender, MatchManager manager)
      throws CommandException {
    List<Component> onlineMutes =
        chat.getMutedUUIDs().stream()
            .filter(u -> (manager.getPlayer(u) != null))
            .map(manager::getPlayer)
            .map(mp -> mp.getName(NameStyle.FANCY))
            .collect(Collectors.toList());
    if (onlineMutes.isEmpty()) {
      throw new CommandException(
          TextTranslations.translateLegacy(
              translatable("moderation.mute.none", NamedTextColor.RED), sender));
    }

    Component names = join(text(", ", NamedTextColor.GRAY), onlineMutes);
    Component message =
        text()
            .append(translatable("moderation.mute.list", NamedTextColor.GOLD))
            .append(text("(", NamedTextColor.GRAY))
            .append(text(onlineMutes.size(), NamedTextColor.YELLOW))
            .append(text("): ", NamedTextColor.GRAY))
            .append(names)
            .build();

    viewer.sendMessage(message);
  }

  @Command(
      aliases = {"unmute", "um"},
      usage = "<player>",
      desc = "Unmute a player",
      perms = Permissions.MUTE)
  public void unMute(Audience viewer, CommandSender sender, Player target, Match match) {
    MatchPlayer targetMatchPlayer = match.getPlayer(target);
    if (chat.isMuted(targetMatchPlayer)) {
      chat.removeMuted(targetMatchPlayer);
      targetMatchPlayer.sendMessage(translatable("moderation.unmute.target", NamedTextColor.GREEN));
      viewer.sendMessage(
          translatable(
              "moderation.unmute.sender",
              NamedTextColor.GRAY,
              targetMatchPlayer.getName(NameStyle.FANCY)));
    } else {
      viewer.sendMessage(
          translatable(
              "moderation.unmute.none",
              NamedTextColor.RED,
              targetMatchPlayer.getName(NameStyle.FANCY)));
    }
  }

  @Command(
      aliases = {"warn", "w"},
      usage = "<player> <reason>",
      desc = "Warn a player for bad behavior",
      perms = Permissions.WARN)
  public void warn(CommandSender sender, Player target, Match match, @Text String reason) {
    MatchPlayer targetMatchPlayer = match.getPlayer(target);
    if (punish(PunishmentType.WARN, targetMatchPlayer, sender, reason, true)) {
      sendModerationWarning(targetMatchPlayer, reason);
    }
  }

  @Command(
      aliases = {"kick", "k"},
      usage = "<player> <reason> -s (silent)",
      desc = "Kick a player from the server",
      flags = "s",
      perms = Permissions.KICK)
  public void kick(
      CommandSender sender,
      Player target,
      Match match,
      @Text String reason,
      @Switch('s') boolean silent) {
    silent = checkSilent(silent, sender);
    MatchPlayer targetMatchPlayer = match.getPlayer(target);
    if (punish(PunishmentType.KICK, targetMatchPlayer, sender, reason, silent)) {

      // Unfreeze players who are kicked
      FreezeMatchModule fmm = match.getModule(FreezeMatchModule.class);
      if (fmm.isFrozen(targetMatchPlayer)) {
        fmm.setFrozen(sender, targetMatchPlayer, false, false);
      }

      target.kickPlayer(
          formatPunishmentScreen(
              PunishmentType.KICK,
              UsernameFormatUtils.formatStaffName(sender, match),
              reason,
              null));
    }
  }

  @Command(
      aliases = {"ban", "permban", "pb"},
      usage = "<player> <reason> -s (silent) -t (time)",
      desc = "Ban an online player from the server",
      flags = "st",
      perms = Permissions.BAN)
  public void ban(
      CommandSender sender,
      Player target,
      Match match,
      @Text String reason,
      @Switch('s') boolean silent,
      @Switch('t') Duration banLength)
      throws CommandException {
    silent = checkSilent(silent, sender);
    boolean tempBan = banLength != null && !banLength.isZero();

    MatchPlayer targetMatchPlayer = match.getPlayer(target);
    Component senderName = UsernameFormatUtils.formatStaffName(sender, match);
    if (punish(
        tempBan ? PunishmentType.TEMP_BAN : PunishmentType.BAN,
        targetMatchPlayer,
        sender,
        reason,
        banLength,
        silent)) {
      banPlayer(
          target.getName(), reason, senderName, tempBan ? Instant.now().plus(banLength) : null);
      cacheRecentBan(target, UsernameFormatUtils.formatStaffName(sender, match));
      target.kickPlayer(
          formatPunishmentScreen(
              tempBan ? PunishmentType.TEMP_BAN : PunishmentType.BAN,
              senderName,
              reason,
              tempBan ? banLength : null));
    }
  }

  @Command(
      aliases = {"ipban", "banip", "ipb"},
      usage = "<player|ip address> <reason> -s (silent)",
      desc = "IP Ban a player from the server",
      flags = "s",
      perms = Permissions.BAN)
  public void ipBan(
      Audience viewer,
      CommandSender sender,
      MatchManager manager,
      String target,
      @Text String reason,
      @Switch('s') boolean silent)
      throws CommandException {
    silent = checkSilent(silent, sender);

    Player targetPlayer = Bukkit.getPlayerExact(target);
    String address = target; // Default address to what was input

    if (targetPlayer != null) {
      // If target is a player, fetch their IP and use that
      address = targetPlayer.getAddress().getAddress().getHostAddress();
    } else if (getBanWithMatchingName(target).isPresent()) {
      address = getBanWithMatchingName(target).get().getAddress();
    }

    // Validate if the IP is a valid IP
    if (InetAddresses.isInetAddress(address)) {
      // Special method for IP Ban
      Bukkit.getBanList(BanList.Type.IP)
          .addBan(
              address,
              reason,
              null,
              TextTranslations.translateLegacy(
                  UsernameFormatUtils.formatStaffName(sender, manager.getMatch(sender)), sender));

      int onlineBans = 0;
      // Check all online players to find those with same IP.
      for (Player player : Bukkit.getOnlinePlayers()) {
        MatchPlayer matchPlayer = manager.getPlayer(player);
        if (player.getAddress().getAddress().getHostAddress().equals(address)) {
          // Kick players with same IP
          if (punish(PunishmentType.BAN, matchPlayer, sender, reason, silent)) {

            // Ban username to prevent rejoining
            banPlayer(
                player.getName(),
                reason,
                UsernameFormatUtils.formatStaffName(sender, matchPlayer.getMatch()),
                null);

            player.kickPlayer(
                formatPunishmentScreen(
                    PunishmentType.BAN,
                    UsernameFormatUtils.formatStaffName(sender, manager.getMatch(sender)),
                    reason,
                    null));
            onlineBans++;
          }
        }
      }

      Component formattedTarget = text(target, NamedTextColor.DARK_AQUA);
      if (onlineBans > 0) {
        viewer.sendWarning(
            translatable(
                "moderation.ipBan.bannedWithAlts",
                formattedTarget,
                text(
                    targetPlayer == null ? onlineBans : Math.max(0, onlineBans - 1),
                    NamedTextColor.AQUA)));
      } else {
        viewer.sendMessage(
            translatable("moderation.ipBan.banned", NamedTextColor.RED, formattedTarget));
      }

    } else {
      viewer.sendMessage(
          translatable(
              "moderation.ipBan.invalidIP",
              NamedTextColor.GRAY,
              text(address, NamedTextColor.RED, TextDecoration.ITALIC)));
    }
  }

  @Command(
      aliases = {"offlineban", "offban"},
      usage = "<player> <reason> -t (length)",
      desc = "Ban an offline player from the server",
      flags = "t",
      perms = Permissions.BAN)
  public void offlineBan(
      CommandSender sender,
      MatchManager manager,
      String target,
      @Text String reason,
      @Switch('t') Duration duration)
      throws CommandException {
    boolean tempBan = duration != null && !duration.isZero();
    PunishmentType type = tempBan ? PunishmentType.TEMP_BAN : PunishmentType.BAN;
    Component punisher = UsernameFormatUtils.formatStaffName(sender, manager.getMatch(sender));

    banPlayer(target, reason, punisher, tempBan ? Instant.now().plus(duration) : null);
    broadcastPunishment(
        type,
        manager.getMatch(sender),
        sender,
        text(target, NamedTextColor.DARK_AQUA),
        reason,
        true,
        duration);

    // Check if target is online, cache and kick after ban
    Player player = Bukkit.getPlayerExact(target);
    if (player != null) {
      cacheRecentBan(player, punisher);
      player.kickPlayer(formatPunishmentScreen(type, punisher, reason, tempBan ? duration : null));
    }
  }

  @Command(
      aliases = {"alts", "listalts"},
      usage = "[target] [page]",
      desc = "List online players on the same IP",
      perms = Permissions.STAFF)
  public void alts(
      Audience viewer,
      CommandSender sender,
      MatchManager manager,
      @Fallback(Type.NULL) Player targetPl,
      @Default("1") int page)
      throws CommandException {
    if (targetPl != null) {
      MatchPlayer target = manager.getPlayer(targetPl);
      List<MatchPlayer> alts = getAltAccounts(targetPl, manager);
      if (alts.isEmpty()) {
        viewer.sendMessage(
            translatable(
                "moderation.alts.noAlts", NamedTextColor.RED, target.getName(NameStyle.FANCY)));
      } else {
        viewer.sendMessage(formatAltAccountList(target, alts));
      }
    } else {
      List<Component> altAccounts = Lists.newArrayList();
      List<MatchPlayer> accountedFor = Lists.newArrayList();

      for (Player player : Bukkit.getOnlinePlayers()) {
        MatchPlayer targetMP = manager.getPlayer(player);
        List<MatchPlayer> alts = getAltAccounts(player, manager);

        if (alts.isEmpty() || accountedFor.contains(targetMP)) {
          continue;
        } else {
          altAccounts.add(formatAltAccountList(targetMP, alts));
          accountedFor.add(targetMP);
          accountedFor.addAll(alts);
        }
      }

      int perPage = 8;
      int pages = (altAccounts.size() + perPage - 1) / perPage;

      Component pageHeader =
          translatable(
              "command.pageHeader",
              NamedTextColor.GRAY,
              text(page, NamedTextColor.DARK_AQUA),
              text(pages, NamedTextColor.DARK_AQUA));

      Component headerText = translatable("moderation.alts.header", NamedTextColor.DARK_AQUA);

      Component header =
          text()
              .append(headerText)
              .append(text(" (", NamedTextColor.GRAY))
              .append(text(altAccounts.size(), NamedTextColor.DARK_AQUA))
              .append(text(") »", NamedTextColor.GRAY))
              .append(pageHeader)
              .build();

      Component formattedHeader =
          TextFormatter.horizontalLineHeading(sender, header, NamedTextColor.BLUE);

      new PrettyPaginatedComponentResults<Component>(formattedHeader, perPage) {
        @Override
        public Component format(Component data, int index) {
          return data;
        }

        @Override
        public Component formatEmpty() throws CommandException {
          throw new CommandException(ChatColor.RED + "No alternate accounts found!");
        }
      }.display(viewer, altAccounts, page);
    }
  }

  @Command(
      aliases = {"baninfo", "lookup", "l"},
      usage = "[player/uuid]",
      desc = "Lookup baninfo about a player",
      perms = Permissions.STAFF)
  public void banInfo(Audience viewer, CommandSender sender, String target)
      throws CommandException {

    if (!XMLUtils.USERNAME_REGEX.matcher(target).matches()) {
      UUID uuid = UUID.fromString(target);
      Username username = PGM.get().getDatastore().getUsername(uuid);
      if (username.getNameLegacy() != null) {
        target = username.getNameLegacy();
      } else {
        throw new CommandException(
            TextTranslations.translateLegacy(
                translatable(
                    "command.notJoinedServer",
                    NamedTextColor.RED,
                    text(target, NamedTextColor.AQUA)),
                sender));
      }
    }

    BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(target);

    if (ban == null
        || ban.getExpiration() != null && ban.getExpiration().toInstant().isBefore(Instant.now())) {
      throw new CommandException(
          TextTranslations.translateLegacy(
              translatable(
                  "moderation.records.lookupNone",
                  NamedTextColor.GRAY,
                  text(target, NamedTextColor.DARK_AQUA)),
              sender));
    }

    Component header =
        text()
            .append(translatable("moderation.records.header", NamedTextColor.GRAY))
            .append(BROADCAST_DIV)
            .append(text(target, NamedTextColor.DARK_AQUA, TextDecoration.ITALIC))
            .build();
    boolean expires = ban.getExpiration() != null;
    Component banType = translatable("moderation.type.ban", NamedTextColor.GOLD);
    Component expireDate = empty();
    if (expires) {
      String length =
          TextTranslations.translateLegacy(
              TemporalComponent.briefNaturalApproximate(
                  ban.getCreated().toInstant(), ban.getExpiration().toInstant()),
              sender);
      Component remaining =
          TemporalComponent.briefNaturalApproximate(Instant.now(), ban.getExpiration().toInstant())
              .color(NamedTextColor.YELLOW);

      banType =
          translatable(
              "moderation.type.temp_ban",
              NamedTextColor.GOLD,
              text(
                  length.lastIndexOf('s') != -1
                      ? length.substring(0, length.lastIndexOf('s'))
                      : length));
      expireDate = translatable("moderation.screen.expires", NamedTextColor.GRAY, remaining);
    }

    Component createdAgo =
        TemporalComponent.relativePastApproximate(ban.getCreated().toInstant())
            .color(NamedTextColor.GRAY);

    Component banTypeFormatted = translatable("moderation.type", NamedTextColor.GRAY, banType);

    Component reason =
        translatable(
            "moderation.records.reason",
            NamedTextColor.GRAY,
            text(ban.getReason(), NamedTextColor.RED));
    Component source =
        text()
            .append(
                translatable(
                    "moderation.screen.signoff",
                    NamedTextColor.GRAY,
                    text(ban.getSource(), NamedTextColor.AQUA)))
            .append(space())
            .append(createdAgo)
            .build();

    viewer.sendMessage(
        TextFormatter.horizontalLineHeading(sender, header, NamedTextColor.DARK_PURPLE));
    viewer.sendMessage(banTypeFormatted);
    viewer.sendMessage(reason);
    viewer.sendMessage(source);
    if (expires) {
      viewer.sendMessage(expireDate);
    }
  }

  private Component formatAltAccountList(MatchPlayer target, List<MatchPlayer> alts) {
    Component names =
        join(
            text(", ", NamedTextColor.GRAY),
            alts.stream().map(mp -> mp.getName(NameStyle.CONCISE)).collect(Collectors.toList()));
    Component size = text(alts.size(), NamedTextColor.YELLOW);

    return text()
        .append(text("[", NamedTextColor.GOLD))
        .append(target.getName(NameStyle.VERBOSE))
        .append(text("] ", NamedTextColor.GOLD))
        .append(text("(", NamedTextColor.GRAY))
        .append(size)
        .append(text("): ", NamedTextColor.GRAY))
        .append(names)
        .build();
  }

  private List<MatchPlayer> getAltAccounts(Player target, MatchManager manager) {
    List<MatchPlayer> sameIPs = Lists.newArrayList();
    String address = target.getAddress().getAddress().getHostAddress();

    for (Player other : Bukkit.getOnlinePlayers()) {
      if (other.getAddress().getAddress().getHostAddress().equals(address)
          && !other.equals(target)) {
        sameIPs.add(manager.getPlayer(other));
      }
    }

    return sameIPs;
  }

  private boolean punish(
      PunishmentType type,
      MatchPlayer target,
      CommandSender issuer,
      String reason,
      boolean silent) {
    return punish(type, target, issuer, reason, Duration.ZERO, silent);
  }

  private boolean punish(
      PunishmentType type,
      MatchPlayer target,
      CommandSender issuer,
      String reason,
      Duration duration,
      boolean silent) {
    PlayerPunishmentEvent event =
        new PlayerPunishmentEvent(
            issuer, target, type, reason, duration == null ? Duration.ZERO : duration, silent);
    target.getMatch().callEvent(event);
    if (event.isCancelled()) {
      if (event.getCancellationReason() != null) {
        Audience.get(issuer).sendMessage(event.getCancellationReason());
      }
    }

    broadcastPunishment(event);

    return !event.isCancelled();
  }

  public enum PunishmentType {
    MUTE(false),
    WARN(false),
    KICK(true),
    BAN(true),
    TEMP_BAN(true);

    private final String PREFIX_TRANSLATE_KEY = "moderation.type.";
    private final String SCREEN_TRANSLATE_KEY = "moderation.screen.";

    private final boolean screen;

    PunishmentType(boolean screen) {
      this.screen = screen;
    }

    public Component getPunishmentPrefix() {
      return translatable(PREFIX_TRANSLATE_KEY + name().toLowerCase(), NamedTextColor.GOLD);
    }

    public Component getPunishmentPrefix(Component time) {
      return translatable(PREFIX_TRANSLATE_KEY + name().toLowerCase(), NamedTextColor.GOLD, time);
    }

    public Component getScreenComponent(Component reason) {
      if (!screen) return empty();
      return translatable(SCREEN_TRANSLATE_KEY + name().toLowerCase(), NamedTextColor.GOLD, reason);
    }
  }

  /*
   * Format Reason
   */
  public static Component formatPunishmentReason(String reason) {
    return text(reason, NamedTextColor.RED);
  }

  /*
   * Formatting of Kick Screens (KICK/BAN/TEMPBAN)
   */
  public static String formatPunishmentScreen(
      PunishmentType type, Component punisher, String reason, @Nullable Duration expires) {
    List<Component> lines = Lists.newArrayList();

    Component header =
        TextFormatter.horizontalLineHeading(
            null, text(PGMConfig.Moderation.getServerName()), NamedTextColor.DARK_GRAY);
    Component footer =
        TextFormatter.horizontalLine(NamedTextColor.DARK_GRAY, LegacyFormatUtils.MAX_CHAT_WIDTH);

    lines.add(header); // Header Line (server name) - START
    lines.add(empty());
    lines.add(type.getScreenComponent(formatPunishmentReason(reason))); // The reason
    lines.add(empty());

    // If punishment expires, inform user when
    if (expires != null) {
      Component timeLeft =
          TemporalComponent.briefNaturalApproximate(Duration.ofSeconds(expires.getSeconds()));
      lines.add(translatable("moderation.screen.expires", NamedTextColor.GRAY, timeLeft));
      lines.add(empty());
    }

    // Staff sign-off - who performed the punishment
    lines.add(translatable("moderation.screen.signoff", NamedTextColor.GRAY, punisher));

    // Link to rules for review by player
    if (PGMConfig.Moderation.isRuleLinkVisible()) {
      Component rules = text(PGMConfig.Moderation.getRulesLink());

      lines.add(empty());
      lines.add(
          translatable("moderation.screen.rulesLink", NamedTextColor.GRAY, rules)); // Link to rules
    }

    // Configurable last line (for appeal message or etc)
    if (PGMConfig.Moderation.isAppealVisible() && type.equals(PunishmentType.BAN)) {
      lines.add(empty());
      lines.add(text(PGMConfig.Moderation.getAppealMessage()));
    }

    lines.add(empty());
    lines.add(footer); // Footer line - END

    return TextTranslations.translateLegacy(join(newline(), lines), null); // TODO add viewer
  }

  /*
   * Sends a formatted title and plays a sound warning a user of their actions
   */
  private void sendModerationWarning(MatchPlayer target, String reason) {
    Component titleWord = translatable("misc.warning", NamedTextColor.DARK_RED);
    Component warningTitle =
        text().append(WARN_SYMBOL).append(titleWord).append(WARN_SYMBOL).build();
    Component subtitle = formatPunishmentReason(reason).color(NamedTextColor.GOLD);

    // Legacy support - Displays a chat message instead of title
    target.sendMessage(
        TextFormatter.horizontalLineHeading(target.getBukkit(), warningTitle, NamedTextColor.GRAY));
    target.sendMessage(empty());
    target.sendMessage(
        TextFormatter.horizontalLineHeading(
            target.getBukkit(),
            subtitle,
            NamedTextColor.YELLOW,
            TextDecoration.OBFUSCATED,
            LegacyFormatUtils.MAX_CHAT_WIDTH));
    target.sendMessage(empty());
    target.sendMessage(
        TextFormatter.horizontalLineHeading(target.getBukkit(), warningTitle, NamedTextColor.GRAY));

    if (!target.isLegacy()) {
      target.showTitle(
          title(
              warningTitle, subtitle, Title.Times.of(fromTicks(5), fromTicks(200), fromTicks(10))));
    }
    target.playSound(WARN_SOUND);
  }

  private void broadcastPunishment(PlayerPunishmentEvent event) {
    broadcastPunishment(
        event.getType(),
        event.getPlayer().getMatch(),
        event.getSender(),
        event.getPlayer().getName(NameStyle.CONCISE),
        event.getReason(),
        event.isSilent(),
        event.getDuration());
  }

  /*
   * Broadcasts a punishment
   */
  private void broadcastPunishment(
      PunishmentType type,
      Match match,
      CommandSender sender,
      Component target,
      String reason,
      boolean silent,
      Duration length) {

    Component prefix = type.getPunishmentPrefix();
    if (length != null && !length.isZero()) {
      String time =
          TextTranslations.translateLegacy(
              TemporalComponent.briefNaturalApproximate(Duration.ofSeconds(length.getSeconds())),
              sender);
      prefix =
          type.getPunishmentPrefix(
              time.lastIndexOf('s') != -1
                  ? text(time.substring(0, time.lastIndexOf('s')), NamedTextColor.GOLD)
                  : empty());
    }

    Component reasonMsg = formatPunishmentReason(reason);
    Component formattedMsg =
        text()
            .append(UsernameFormatUtils.formatStaffName(sender, match))
            .append(BROADCAST_DIV)
            .append(prefix)
            .append(BROADCAST_DIV)
            .append(target)
            .append(BROADCAST_DIV)
            .append(reasonMsg)
            .build();

    if (!silent) {
      match.sendMessage(formattedMsg);
    } else {
      // When -s flag is present, only broadcast to staff
      ChatDispatcher.broadcastAdminChatMessage(formattedMsg, match);
    }
  }

  private static class BannedAccountInfo {
    private Instant time;
    private String userName;
    private String address;
    private Component punisher;

    public BannedAccountInfo(Player player, Component punisher) {
      this.time = Instant.now();
      this.userName = player.getName();
      this.address = player.getAddress().getAddress().getHostAddress();
      this.punisher = punisher;
    }

    public String getUserName() {
      return userName;
    }

    public String getAddress() {
      return address;
    }

    public boolean isSameAddress(String other) {
      return address.equals(other);
    }

    public Component getPunisher() {
      return punisher;
    }

    public Component getHoverMessage() {
      Component timeAgo =
          TemporalComponent.relativePastApproximate(time).color(NamedTextColor.DARK_AQUA);
      return translatable(
          "moderation.similarIP.hover", NamedTextColor.GRAY, getPunisher(), timeAgo);
    }
  }

  /*
   * Bukkit method of banning players
   * NOTE: Will use this if not handled by other plugins
   */
  private void banPlayer(
      String target, String reason, Component source, @Nullable Instant expires) {
    Bukkit.getBanList(BanList.Type.NAME)
        .addBan(
            target,
            reason,
            expires != null ? Date.from(expires) : null,
            TextTranslations.translateLegacy(source, null));
  }

  // On login of accounts whose IP match a recently banned player, alert staff.
  @EventHandler(priority = EventPriority.MONITOR)
  public void onLogin(PlayerJoinEvent event) {
    Optional<BannedAccountInfo> ban =
        getBanWithMatchingIP(event.getPlayer().getAddress().getAddress().getHostAddress());

    ban.ifPresent(
        info -> {
          if (!isBanStillValid(info.getUserName())) {
            removeCachedBan(info);
            return;
          }

          final MatchPlayer matchPlayer = manager.getPlayer(event.getPlayer());
          final Match match = matchPlayer.getMatch();

          ChatDispatcher.broadcastAdminChatMessage(
              formatAltAccountBroadcast(info, matchPlayer), match, Optional.of(ALT_SOUND));
        });
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPreLogin(PlayerLoginEvent event) {
    // Format kick screen for banned players
    if (event.getResult().equals(Result.KICK_BANNED)) {
      String formatted =
          getPunishmentScreenFromName(event.getPlayer(), event.getPlayer().getName());
      if (formatted != null) {
        event.setKickMessage(formatted);
      }
    }
  }

  public String getPunishmentScreenFromName(Player viewer, String name) {
    if (isBanStillValid(name)) {
      BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(name);
      PunishmentType type =
          ban.getExpiration() != null ? PunishmentType.TEMP_BAN : PunishmentType.BAN;

      Duration length =
          type.equals(PunishmentType.TEMP_BAN)
              ? Duration.between(Instant.now(), ban.getExpiration().toInstant())
              : null;

      return formatPunishmentScreen(
          type, text(ban.getSource(), NamedTextColor.AQUA), ban.getReason(), length);
    }
    return null;
  }

  private boolean isBanStillValid(String name) {
    return Bukkit.getBanList(BanList.Type.NAME).isBanned(name);
  }

  private Component formatAltAccountBroadcast(BannedAccountInfo info, MatchPlayer player) {
    return text()
        .append(
            translatable(
                "moderation.similarIP.loginEvent",
                NamedTextColor.RED,
                player.getName(NameStyle.FANCY),
                text(info.getUserName(), NamedTextColor.DARK_AQUA)))
        .hoverEvent(showText(info.getHoverMessage()))
        .build();
  }

  // Force vanished players to silent broadcast
  private boolean checkSilent(boolean silent, CommandSender sender) {
    if (!silent && sender instanceof Player && vanish.isVanished(((Player) sender).getUniqueId())) {
      silent = true;
    }
    return silent;
  }
}
