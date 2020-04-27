package tc.oc.pgm.community.commands;

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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
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
import tc.oc.pgm.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.community.events.PlayerPunishmentEvent;
import tc.oc.pgm.community.modules.FreezeMatchModule;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.chat.Sound;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.component.ComponentUtils;
import tc.oc.pgm.util.component.Components;
import tc.oc.pgm.util.component.PeriodFormats;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.xml.XMLUtils;

public class ModerationCommands implements Listener {

  private static final Sound WARN_SOUND = new Sound("mob.enderdragon.growl", 1f, 1f);

  private static final Component WARN_SYMBOL =
      new PersonalizedText(" \u26a0 ").color(ChatColor.YELLOW);
  private static final Component BROADCAST_DIV =
      new PersonalizedText(" \u00BB ").color(ChatColor.GRAY);

  private final ChatDispatcher chat;
  private final MatchManager manager;

  private final List<BannedAccountInfo> recentBans;

  public ModerationCommands(ChatDispatcher chat, MatchManager manager) {
    this.chat = chat;
    this.manager = manager;
    this.recentBans = Lists.newArrayList();
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
  public void staff(CommandSender sender, Match match) {
    // List of online staff based off of permission
    List<Component> onlineStaff =
        match.getPlayers().stream()
            .filter(player -> player.getBukkit().hasPermission(Permissions.STAFF))
            .map(player -> player.getStyledName(NameStyle.FANCY))
            .collect(Collectors.toList());

    // FORMAT: Online Staff ({count}): {names}
    Component staffCount =
        new PersonalizedText(Integer.toString(onlineStaff.size()))
            .color(onlineStaff.isEmpty() ? ChatColor.RED : ChatColor.AQUA);

    Component content =
        onlineStaff.isEmpty()
            ? new PersonalizedTranslatable("moderation.staff.empty")
                .getPersonalizedText()
                .color(ChatColor.RED)
            : new Component(
                Components.join(new PersonalizedText(", ").color(ChatColor.GRAY), onlineStaff));

    Component staff =
        new PersonalizedTranslatable("moderation.staff.name", staffCount, content)
            .getPersonalizedText()
            .color(ChatColor.GRAY);

    // Send message
    sender.sendMessage(staff);
  }

  @Command(
      aliases = {"frozenlist", "fls", "flist"},
      desc = "View a list of frozen players",
      perms = Permissions.STAFF)
  public void sendFrozenList(CommandSender sender, Match match) {
    FreezeMatchModule fmm = match.getModule(FreezeMatchModule.class);

    if (fmm.getFrozenPlayers().isEmpty() && fmm.getOfflineFrozenCount() < 1) {
      sender.sendMessage(
          new PersonalizedTranslatable("moderation.freeze.frozenList.none")
              .getPersonalizedText()
              .color(ChatColor.RED));
      return;
    }

    // Online Players
    if (!fmm.getFrozenPlayers().isEmpty()) {
      Component names =
          new Component(
              Components.join(
                  new PersonalizedText(", ").color(ChatColor.GRAY),
                  fmm.getFrozenPlayers().stream()
                      .map(m -> m.getStyledName(NameStyle.FANCY))
                      .collect(Collectors.toList())));
      sender.sendMessage(
          formatFrozenList(
              "moderation.freeze.frozenList.online", fmm.getFrozenPlayers().size(), names));
    }

    // Offline Players
    if (fmm.getOfflineFrozenCount() > 0) {
      Component names = new PersonalizedText(fmm.getOfflineFrozenNames());
      sender.sendMessage(
          formatFrozenList(
              "moderation.freeze.frozenList.offline", fmm.getOfflineFrozenCount(), names));
    }
  }

  private Component formatFrozenList(String key, int count, Component names) {
    return new PersonalizedTranslatable(
            key, new PersonalizedText(Integer.toString(count), ChatColor.AQUA), names)
        .getPersonalizedText()
        .color(ChatColor.GRAY);
  }

  @Command(
      aliases = {"freeze", "fz", "f"},
      usage = "<player>",
      desc = "Freeze a player",
      perms = Permissions.STAFF)
  public void freeze(CommandSender sender, Match match, Player target) throws CommandException {
    setFreeze(sender, match, target, true);
  }

  @Command(
      aliases = {"unfreeze", "uf"},
      usage = "<player>",
      desc = "Unfreeze a player",
      perms = Permissions.STAFF)
  public void unFreeze(CommandSender sender, Match match, Player target) throws CommandException {
    setFreeze(sender, match, target, false);
  }

  private void setFreeze(CommandSender sender, Match match, Player target, boolean freeze)
      throws CommandException {
    FreezeMatchModule fmm = match.getModule(FreezeMatchModule.class);
    MatchPlayer player = match.getPlayer(target);

    Component alreadyFrozen =
        new PersonalizedTranslatable(
                "moderation.freeze.alreadyFrozen",
                match.getPlayer(target).getStyledName(NameStyle.FANCY))
            .getPersonalizedText()
            .color(ChatColor.GRAY);
    Component alreadyThawed =
        new PersonalizedTranslatable(
                "moderation.freeze.alreadyThawed",
                match.getPlayer(target).getStyledName(NameStyle.FANCY))
            .getPersonalizedText()
            .color(ChatColor.GRAY);

    if (player != null) {
      if (fmm.isFrozen(player) && freeze) {
        throw new CommandException(ComponentRenderers.toLegacyText(alreadyFrozen, sender));
      } else if (!fmm.isFrozen(player) && !freeze) {
        throw new CommandException(ComponentRenderers.toLegacyText(alreadyThawed, sender));
      } else {
        fmm.setFrozen(sender, player, freeze);
      }
    }
  }

  @Command(
      aliases = {"mute", "m"},
      usage = "<player> <reason>",
      desc = "Mute a player",
      perms = Permissions.MUTE)
  public void mute(CommandSender sender, Player target, Match match, @Text String reason) {
    MatchPlayer targetMatchPlayer = match.getPlayer(target);
    if (chat.isMuted(targetMatchPlayer)) {
      sender.sendMessage(
          new PersonalizedTranslatable(
                  "moderation.mute.existing", targetMatchPlayer.getStyledName(NameStyle.FANCY))
              .getPersonalizedText()
              .color(ChatColor.RED));
      return;
    }

    // Send a warning to the player to identify mute reason
    warn(sender, target, match, reason);

    if (punish(PunishmentType.MUTE, targetMatchPlayer, sender, reason, true)) {
      chat.addMuted(targetMatchPlayer);
    }
  }

  @Command(
      aliases = {"mutes", "mutelist"},
      desc = "List of muted players",
      perms = Permissions.MUTE)
  public void listMutes(CommandSender sender, MatchManager manager) throws CommandException {
    Set<UUID> mutes = chat.getMutedUUIDs();
    List<Component> onlineMutes =
        mutes.stream()
            .filter(u -> (manager.getPlayer(u) != null))
            .map(manager::getPlayer)
            .map(p -> p.getStyledName(NameStyle.CONCISE))
            .collect(Collectors.toList());
    if (onlineMutes.isEmpty()) {
      throw new CommandException(
          ComponentRenderers.toLegacyText(
              new PersonalizedTranslatable("moderation.mute.none"), sender));
    }

    Component names =
        new Component(
            Components.join(new PersonalizedText(", ").color(ChatColor.GRAY), onlineMutes));
    Component message =
        new PersonalizedText(
            new PersonalizedTranslatable("moderation.mute.list")
                .getPersonalizedText()
                .color(ChatColor.GOLD),
            new PersonalizedText(" (").color(ChatColor.GRAY),
            new PersonalizedText(Integer.toString(onlineMutes.size())).color(ChatColor.YELLOW),
            new PersonalizedText("): ").color(ChatColor.GRAY),
            names);
    sender.sendMessage(message);
  }

  @Command(
      aliases = {"unmute", "um"},
      usage = "<player>",
      desc = "Unmute a player",
      perms = Permissions.MUTE)
  public void unMute(CommandSender sender, Player target, Match match) {
    MatchPlayer targetMatchPlayer = match.getPlayer(target);
    if (chat.isMuted(targetMatchPlayer)) {
      chat.removeMuted(targetMatchPlayer);

      targetMatchPlayer.sendMessage(
          new PersonalizedTranslatable("moderation.unmute.target")
              .getPersonalizedText()
              .color(ChatColor.GREEN));

      sender.sendMessage(
          new PersonalizedTranslatable(
                  "moderation.unmute.sender", targetMatchPlayer.getStyledName(NameStyle.FANCY))
              .color(ChatColor.GRAY));
    } else {
      sender.sendMessage(
          new PersonalizedTranslatable(
                  "moderation.unmute.none", targetMatchPlayer.getStyledName(NameStyle.FANCY))
              .getPersonalizedText()
              .color(ChatColor.RED));
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
      sendWarning(targetMatchPlayer, reason);
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
    MatchPlayer targetMatchPlayer = match.getPlayer(target);
    if (punish(PunishmentType.KICK, targetMatchPlayer, sender, reason, silent)) {
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
      flags = "s",
      perms = Permissions.BAN)
  public void ban(
      CommandSender sender,
      Player target,
      Match match,
      @Text String reason,
      @Switch('s') boolean silent,
      @Switch('t') Duration banLength)
      throws CommandException {
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
      CommandSender sender,
      MatchManager manager,
      String target,
      @Text String reason,
      @Switch('s') boolean silent)
      throws CommandException {
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
              ComponentRenderers.toLegacyText(
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

      Component formattedTarget = new PersonalizedText(target).color(ChatColor.DARK_AQUA);
      if (onlineBans > 0) {
        sender.sendMessage(
            new PersonalizedTranslatable(
                    "moderation.ipBan.bannedWithAlts",
                    formattedTarget,
                    new PersonalizedText(Integer.toString(onlineBans)).color(ChatColor.AQUA))
                .getPersonalizedText()
                .color(ChatColor.RED));
      } else {
        sender.sendMessage(
            new PersonalizedTranslatable("moderation.ipBan.banned", formattedTarget)
                .getPersonalizedText()
                .color(ChatColor.RED));
      }

    } else {
      sender.sendMessage(
          new PersonalizedTranslatable(
                  "moderation.ipBan.invalidIP",
                  new PersonalizedText(address).color(ChatColor.RED).italic(true))
              .color(ChatColor.GRAY));
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
        new PersonalizedText(target).color(ChatColor.DARK_AQUA),
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
      Audience audience,
      CommandSender sender,
      MatchManager manager,
      @Fallback(Type.NULL) Player targetPl,
      @Default("1") int page)
      throws CommandException {
    if (targetPl != null) {
      MatchPlayer target = manager.getPlayer(targetPl);
      List<MatchPlayer> alts = getAltAccounts(targetPl, manager);
      if (alts.isEmpty()) {
        sender.sendMessage(
            new PersonalizedTranslatable(
                    "moderation.alts.noAlts", target.getStyledName(NameStyle.FANCY))
                .getPersonalizedText()
                .color(ChatColor.RED));
      } else {
        sender.sendMessage(formatAltAccountList(target, alts));
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

      Component pageNum =
          new PersonalizedTranslatable(
                  "command.simplePageHeader",
                  new PersonalizedText(Integer.toString(page)).color(ChatColor.DARK_AQUA),
                  new PersonalizedText(Integer.toString(pages)).color(ChatColor.DARK_AQUA))
              .getPersonalizedText()
              .color(ChatColor.GRAY);

      Component headerText =
          new PersonalizedTranslatable("moderation.alts.header")
              .getPersonalizedText()
              .color(ChatColor.DARK_AQUA);

      Component header =
          new PersonalizedText(
              headerText,
              new PersonalizedText(" (").color(ChatColor.GRAY),
              new PersonalizedText(Integer.toString(altAccounts.size())).color(ChatColor.DARK_AQUA),
              new PersonalizedText(") » Page ").color(ChatColor.GRAY),
              pageNum);

      Component formattedHeader =
          new PersonalizedText(
              ComponentUtils.horizontalLineHeading(
                  ComponentRenderers.toLegacyText(header, sender), ChatColor.BLUE));

      new PrettyPaginatedComponentResults<Component>(formattedHeader, perPage) {
        @Override
        public Component format(Component data, int index) {
          return data;
        }

        @Override
        public Component formatEmpty() throws CommandException {
          throw new CommandException("No alternate accounts found!");
        }
      }.display(audience, altAccounts, page);
    }
  }

  @Command(
      aliases = {"baninfo", "lookup", "l"},
      usage = "[player/uuid]",
      desc = "Lookup baninfo about a player",
      perms = Permissions.STAFF)
  public void banInfo(CommandSender sender, String target) throws CommandException {

    if (!XMLUtils.USERNAME_REGEX.matcher(target).matches()) {
      UUID uuid = UUID.fromString(target);
      Username username = PGM.get().getDatastore().getUsername(uuid);
      if (username.getName() != null) {
        target = username.getName();
      } else {
        throw new CommandException(
            ComponentRenderers.toLegacyText(
                new PersonalizedTranslatable(
                        "command.notJoinedServer",
                        new PersonalizedText(target).color(ChatColor.AQUA))
                    .getPersonalizedText()
                    .color(ChatColor.RED),
                sender));
      }
    }

    BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(target);

    if (ban == null
        || ban.getExpiration() != null && ban.getExpiration().toInstant().isBefore(Instant.now())) {
      throw new CommandException(
          ComponentRenderers.toLegacyText(
              new PersonalizedTranslatable(
                  "moderation.records.lookupNone",
                  new PersonalizedText(target).color(ChatColor.DARK_AQUA)),
              sender));
    }

    Component header =
        new PersonalizedText(
            new PersonalizedTranslatable("moderation.records.header")
                .getPersonalizedText()
                .color(ChatColor.GRAY),
            new PersonalizedText(" » ").color(ChatColor.GOLD),
            new PersonalizedText(target).color(ChatColor.DARK_AQUA).italic(true));
    new PersonalizedTranslatable(
            "moderation.records.header", new PersonalizedText(target).color(ChatColor.DARK_AQUA))
        .getPersonalizedText()
        .color(ChatColor.DARK_GREEN);
    boolean expires = ban.getExpiration() != null;
    Component banType =
        new PersonalizedTranslatable("moderation.type.ban")
            .getPersonalizedText()
            .color(ChatColor.GOLD);
    Component expireDate = Components.blank();
    if (expires) {
      String length =
          ComponentRenderers.toLegacyText(
              PeriodFormats.briefNaturalApproximate(
                  java.time.Instant.ofEpochSecond(ban.getCreated().toInstant().getEpochSecond()),
                  java.time.Instant.ofEpochSecond(
                      ban.getExpiration().toInstant().getEpochSecond())),
              sender);
      String remaining =
          ComponentRenderers.toLegacyText(
              PeriodFormats.briefNaturalApproximate(
                  java.time.Instant.now(),
                  java.time.Instant.ofEpochSecond(
                      ban.getExpiration().toInstant().getEpochSecond())),
              sender);

      banType =
          new PersonalizedTranslatable(
                  "moderation.type.temp_ban",
                  new PersonalizedText(
                      length.lastIndexOf('s') != -1
                          ? length.substring(0, length.lastIndexOf('s'))
                          : length))
              .getPersonalizedText()
              .color(ChatColor.GOLD);
      expireDate =
          new PersonalizedTranslatable(
                  "moderation.screen.expires",
                  new PersonalizedText(remaining).color(ChatColor.YELLOW))
              .getPersonalizedText()
              .color(ChatColor.GRAY);
    }

    String createdAgo =
        ComponentRenderers.toLegacyText(
            PeriodFormats.relativePastApproximate(
                java.time.Instant.ofEpochSecond(ban.getCreated().toInstant().getEpochSecond())),
            sender);

    Component banTypeFormatted =
        new PersonalizedTranslatable("moderation.type.mainComponent", banType)
            .getPersonalizedText()
            .color(ChatColor.GRAY);

    Component reason =
        new PersonalizedTranslatable(
                "moderation.records.reason",
                new PersonalizedText(ban.getReason()).color(ChatColor.RED))
            .getPersonalizedText()
            .color(ChatColor.GRAY);
    Component source =
        new PersonalizedText(
            new PersonalizedTranslatable(
                    "moderation.screen.signoff",
                    new PersonalizedText(ban.getSource()).color(ChatColor.AQUA))
                .getPersonalizedText()
                .color(ChatColor.GRAY),
            Components.space(),
            new PersonalizedText(createdAgo).color(ChatColor.GRAY));

    sender.sendMessage(
        ComponentUtils.horizontalLineHeading(
            ComponentRenderers.toLegacyText(header, sender), ChatColor.DARK_PURPLE));
    sender.sendMessage(banTypeFormatted);
    sender.sendMessage(reason);
    sender.sendMessage(source);
    if (expires) {
      sender.sendMessage(expireDate);
    }
  }

  private Component formatAltAccountList(MatchPlayer target, List<MatchPlayer> alts) {
    BaseComponent names =
        Components.join(
            new PersonalizedText(", ").color(ChatColor.GRAY),
            alts.stream()
                .map(mp -> mp.getStyledName(NameStyle.CONCISE))
                .collect(Collectors.toList()));
    Component size = new PersonalizedText(Integer.toString(alts.size())).color(ChatColor.YELLOW);

    return new PersonalizedText(
        new PersonalizedText("[").color(ChatColor.GOLD),
        target.getStyledName(NameStyle.VERBOSE),
        new PersonalizedText("] ").color(ChatColor.GOLD),
        new PersonalizedText("(").color(ChatColor.GRAY),
        size,
        new PersonalizedText("): ").color(ChatColor.GRAY),
        new Component(names));
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
      if (event.getCancelMessage() != null) {
        issuer.sendMessage(event.getCancelMessage());
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

    private String PREFIX_TRANSLATE_KEY = "moderation.type.";
    private String SCREEN_TRANSLATE_KEY = "moderation.screen.";

    private final boolean screen;

    PunishmentType(boolean screen) {
      this.screen = screen;
    }

    public Component getPunishmentPrefix() {
      return new PersonalizedTranslatable(PREFIX_TRANSLATE_KEY + name().toLowerCase())
          .getPersonalizedText()
          .color(ChatColor.GOLD);
    }

    public Component getPunishmentPrefix(Component time) {
      return new PersonalizedTranslatable(PREFIX_TRANSLATE_KEY + name().toLowerCase(), time)
          .getPersonalizedText()
          .color(ChatColor.GOLD);
    }

    public Component getScreenComponent(Component reason) {
      if (!screen) return Components.blank();
      return new PersonalizedTranslatable(SCREEN_TRANSLATE_KEY + name().toLowerCase(), reason)
          .getPersonalizedText()
          .color(ChatColor.GOLD);
    }
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
  public static String formatPunishmentScreen(
      PunishmentType type, Component punisher, String reason, @Nullable Duration expires) {
    List<Component> lines = Lists.newArrayList();

    Component header =
        new PersonalizedText(
            ComponentUtils.horizontalLineHeading(
                Config.Moderation.getServerName(), ChatColor.DARK_GRAY));

    Component footer =
        new PersonalizedText(
            ComponentUtils.horizontalLine(ChatColor.DARK_GRAY, ComponentUtils.MAX_CHAT_WIDTH));

    lines.add(header); // Header Line (server name) - START
    lines.add(Components.blank());
    lines.add(type.getScreenComponent(formatPunishmentReason(reason))); // The reason
    lines.add(Components.blank());

    // If punishment expires, inform user when
    if (expires != null) {
      Component timeLeft =
          PeriodFormats.briefNaturalApproximate(java.time.Duration.ofSeconds(expires.getSeconds()));
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

    // Link to rules for review by player
    if (Config.Moderation.isRuleLinkVisible()) {
      Component rules = new PersonalizedText(Config.Moderation.getRulesLink());

      lines.add(Components.blank());
      lines.add(
          new PersonalizedTranslatable("moderation.screen.rulesLink", rules)
              .getPersonalizedText()
              .color(ChatColor.GRAY)); // A link to the rules
    }

    // Configurable last line (for appeal message or etc)
    if (Config.Moderation.isAppealVisible() && type.equals(PunishmentType.BAN)) {
      lines.add(Components.blank());
      lines.add(new PersonalizedText(Config.Moderation.getAppealMessage()));
    }

    lines.add(Components.blank());
    lines.add(footer); // Footer line - END

    return Components.join(new PersonalizedText("\n" + ChatColor.RESET), lines).toLegacyText();
  }

  /*
   * Sends a formatted title and plays a sound warning a user of their actions
   */
  private void sendWarning(MatchPlayer target, String reason) {
    Component titleWord =
        new PersonalizedTranslatable("moderation.warning")
            .getPersonalizedText()
            .color(ChatColor.DARK_RED);
    Component title = new PersonalizedText(WARN_SYMBOL, titleWord, WARN_SYMBOL);
    Component subtitle = formatPunishmentReason(reason).color(ChatColor.GOLD);

    target.showTitle(title, subtitle, 5, 200, 10);
    target.playSound(WARN_SOUND);
  }

  private void broadcastPunishment(PlayerPunishmentEvent event) {
    broadcastPunishment(
        event.getType(),
        event.getPlayer().getMatch(),
        event.getSender(),
        event.getPlayer().getStyledName(NameStyle.CONCISE),
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
          ComponentRenderers.toLegacyText(
              PeriodFormats.briefNaturalApproximate(
                  java.time.Duration.ofSeconds(length.getSeconds())),
              sender);
      prefix =
          type.getPunishmentPrefix(
              time.lastIndexOf('s') != -1
                  ? new PersonalizedText(time.substring(0, time.lastIndexOf('s')))
                      .color(ChatColor.GOLD)
                  : Components.blank());
    }

    Component reasonMsg = ModerationCommands.formatPunishmentReason(reason);
    Component formattedMsg =
        new PersonalizedText(
            UsernameFormatUtils.formatStaffName(sender, match),
            BROADCAST_DIV,
            prefix,
            BROADCAST_DIV,
            target,
            BROADCAST_DIV,
            reasonMsg);

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
          PeriodFormats.relativePastApproximate(java.time.Instant.ofEpochMilli(time.toEpochMilli()))
              .color(ChatColor.DARK_AQUA);

      return new PersonalizedTranslatable("moderation.similarIP.hover", getPunisher(), timeAgo)
          .getPersonalizedText()
          .color(ChatColor.GRAY);
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
            ComponentRenderers.toLegacyText(source, Bukkit.getConsoleSender()));
  }

  // On login of accounts whose IP match a recently banned player, alert staff.
  @EventHandler(priority = EventPriority.MONITOR)
  public void onLogin(PlayerJoinEvent event) {
    Optional<BannedAccountInfo> ban =
        getBanWithMatchingIP(event.getPlayer().getAddress().getAddress().getHostAddress());

    ban.ifPresent(
        info -> {
          if (!isBanStillValid(event.getPlayer())) {
            removeCachedBan(info);
            return;
          }

          final MatchPlayer matchPlayer = manager.getPlayer(event.getPlayer());
          final Match match = matchPlayer.getMatch();

          ChatDispatcher.broadcastAdminChatMessage(
              formatAltAccountBroadcast(info, matchPlayer), match);
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
          type,
          new PersonalizedText(ban.getSource()).color(ChatColor.AQUA),
          ban.getReason(),
          length);
    }
    return null;
  }

  private boolean isBanStillValid(String name) {
    return Bukkit.getBanList(BanList.Type.NAME).isBanned(name);
  }

  private boolean isBanStillValid(Player player) {
    return isBanStillValid(player.getName());
  }

  private Component formatAltAccountBroadcast(BannedAccountInfo info, MatchPlayer player) {
    Component message =
        new PersonalizedTranslatable(
                "moderation.similarIP.loginEvent",
                player.getStyledName(NameStyle.FANCY),
                new PersonalizedText(info.getUserName()).color(ChatColor.DARK_AQUA))
            .getPersonalizedText()
            .color(ChatColor.GRAY);
    return message.hoverEvent(HoverEvent.Action.SHOW_TEXT, info.getHoverMessage().render());
  }
}
