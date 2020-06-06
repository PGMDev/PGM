package tc.oc.pgm.util.text.types;

import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.event.HoverEvent.Action;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextParser;

/** PlayerComponent is used to format player names in a consistent manner with optional styling */
public interface PlayerComponent {

  static Component UNKNOWN =
      TranslatableComponent.of("misc.unknown", TextColor.DARK_AQUA, TextDecoration.ITALIC);

  static Component of(Player player, NameStyle style) {
    return of(player, "", style);
  }

  static Component of(@Nullable Player player, String defName, NameStyle style) {
    TextComponent.Builder component = TextComponent.builder();

    // Offline player
    if (player == null || !player.isOnline()) {
      TextComponent name = TextComponent.of(defName);
      if (style != NameStyle.PLAIN) name = name.color(TextColor.DARK_AQUA);
      component.append(name);
      return component.build();
    }

    Component usernameComponent;
    String realName = player.getName();
    String displayName = player.getDisplayName();

    if (!style.showPrefix) {
      displayName = displayName.substring(displayName.indexOf(realName) - 2);
    }

    if (style.showDeath && isDead(player)) {
      displayName =
          displayName.replaceFirst(realName, ChatColor.DARK_GRAY + realName + ChatColor.RESET);
    }

    if (style.showVanish && isVanished(player)) {
      displayName =
          displayName.replaceFirst(realName, ChatColor.STRIKETHROUGH + realName + ChatColor.RESET);
    }

    usernameComponent = TextParser.parseComponent(displayName);
    component.append(usernameComponent);

    if (style.teleport) {
      component.hoverEvent(
          HoverEvent.of(
              Action.SHOW_TEXT,
              TranslatableComponent.of("misc.teleportTo", TextColor.GRAY).args(usernameComponent)));
      component.clickEvent(ClickEvent.runCommand("/tp " + player.getName()));
    }

    return component.build();
  }

  static Component of(UUID playerId, NameStyle style) {
    Player player = Bukkit.getPlayer(playerId);
    return player != null ? of(player, style) : UNKNOWN;
  }

  static Component of(CommandSender sender, NameStyle style) {
    return sender instanceof Player
        ? of((Player) sender, style)
        : TranslatableComponent.of("misc.console", TextColor.DARK_AQUA);
  }

  static boolean isVanished(Player player) {
    return player.hasMetadata("isVanished");
  }

  static boolean isDead(Player player) {
    return player.hasMetadata("isDead") || player.isDead();
  }
}
