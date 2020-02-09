package tc.oc.pgm.listeners;

import java.util.Date;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.commands.ModerationCommands;
import tc.oc.pgm.commands.ModerationCommands.PunishmentType;
import tc.oc.pgm.events.PlayerPunishmentEvent;
import tc.oc.pgm.events.PlayerTimedPunishmentEvent;
import tc.oc.util.components.Components;

public class ModerationListener implements Listener {

  private static final Component BROADCAST_DIV =
      new PersonalizedText(" \u00BB ").color(ChatColor.GOLD);

  @EventHandler
  public void onPunishmentBroadcast(PlayerPunishmentEvent event) {
    if (!event.isCancelled()) {
      broadcastPunishment(
          event.getType(),
          event.getPlayer().getMatch(),
          event.getSender(),
          event.getPlayer(),
          event.getReason(),
          event.isSilent());
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBan(PlayerPunishmentEvent event) {
    if (!event.isCancelled()) {
      if (event.getType().equals(PunishmentType.BAN)) {
        banPlayer(event, null);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onTempBan(PlayerTimedPunishmentEvent event) {
    if (!event.isCancelled()) {
      banPlayer(event, event.getExpiryDate());
    }
  }

  private void banPlayer(PlayerPunishmentEvent event, @Nullable Date expires) {
    Bukkit.getBanList(BanList.Type.NAME)
        .addBan(
            event.getPlayer().getBukkit().getName(),
            event.getReason(),
            expires,
            event.getSender().getName());
  }

  private void broadcastPunishment(
      PunishmentType type,
      Match match,
      CommandSender sender,
      MatchPlayer target,
      String reason,
      boolean silent) {
    Component prefix =
        new PersonalizedTranslatable("moderation.punishment.prefix", type.getPunishmentPrefix())
            .getPersonalizedText()
            .color(ChatColor.GOLD);
    Component targetName = target.getStyledName(NameStyle.FANCY);
    Component reasonMsg = ModerationCommands.formatPunishmentReason(reason);
    Component formattedMsg =
        new PersonalizedText(
            prefix,
            Components.space(),
            ModerationCommands.formatPunisherName(sender, match),
            BROADCAST_DIV,
            targetName,
            BROADCAST_DIV,
            reasonMsg);

    if (!silent) {
      match.sendMessage(formattedMsg);
    } else {
      // if silent flag present, only notify sender
      sender.sendMessage(formattedMsg);
    }
  }
}
