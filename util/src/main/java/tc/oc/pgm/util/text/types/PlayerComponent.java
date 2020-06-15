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
import tc.oc.pgm.util.text.TextFormatter;

/** PlayerComponent is used to format player names in a consistent manner with optional styling */
public interface PlayerComponent {

  static Component UNKNOWN =
      TranslatableComponent.of("misc.unknown", TextColor.DARK_AQUA, TextDecoration.ITALIC);

  static Component of(Player player, NameStyle style) {
    return of(player, "", style);
  }

  static Component of(@Nullable Player player, String defName, NameStyle style) {

    // Offline player or not visible
    if ((player == null || !player.isOnline())) {
      return formatOffline(defName, style == NameStyle.PLAIN);
    }

    // For name styles that don't allow vanished, make vanished appear offline
    if (!style.showVanish && isVanished(player)) {
      return formatOffline(player.getName(), style == NameStyle.PLAIN);
    }

    switch (style) {
      case COLOR:
        return formatTeleport(formatColor(player), player.getName());
      case CONCISE:
        return formatConcise(player);
      case FANCY:
        return formatFancy(player);
      case PLAIN:
        return formatPlain(player);
      case TAB:
        return formatTab(player);
      case VERBOSE:
        return formatVerbose(player);
      default:
        return formatPlain(player);
    }
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

  // What an offline or vanished username renders as
  static Component formatOffline(String name, boolean plain) {
    TextComponent component = TextComponent.of(name);
    if (!plain) component = component.color(TextColor.DARK_AQUA);
    return component;
  }

  // No color or formatting, simply the name
  static Component formatPlain(Player player) {
    return TextComponent.of(player.getName());
  }

  // Color only
  static Component formatColor(Player player) {
    String displayName = player.getDisplayName();
    char colorChar = displayName.charAt((displayName.indexOf(player.getName()) - 1));
    TextColor color = TextFormatter.convert(ChatColor.getByChar(colorChar));
    return TextComponent.of(player.getName(), color);
  }

  // Color, flair & teleport
  static Component formatFancy(Player player) {
    Component prefix = getPrefixComponent(player);
    Component colorName = formatColor(player);
    return formatTeleport(prefix.append(colorName), player.getName());
  }

  // Color, flair, death status, and vanish
  static Component formatTab(Player player) {
    Component prefix = getPrefixComponent(player);
    Component colorName = formatColor(player);

    if (isDead(player)) {
      colorName = colorName.color(TextColor.DARK_GRAY);
    }

    if (isVanished(player)) {
      colorName = colorName.decoration(TextDecoration.STRIKETHROUGH, true);
    }

    return prefix.append(colorName);
  }

  // Color, flair, and vanish status
  static Component formatConcise(Player player) {
    Component prefix = getPrefixComponent(player);
    Component colorName = formatColor(player);

    if (isVanished(player)) {
      colorName = colorName.decoration(TextDecoration.STRIKETHROUGH, true);
    }

    return prefix.append(colorName);
  }

  // Color, flair, vanished, and teleport
  static Component formatVerbose(Player player) {
    return formatTeleport(formatConcise(player), player.getName());
  }

  /**
   * Get the player's prefix as a {@link Component}
   *
   * @param player The player
   * @return a component with a player's prefix
   */
  static Component getPrefixComponent(Player player) {
    String realName = player.getName();
    String displayName = player.getDisplayName();
    String prefix = displayName.substring(0, displayName.indexOf(realName) - 2);

    TextComponent.Builder prefixComponent = TextComponent.builder();
    boolean isColor = false;
    TextColor color = null;
    for (int i = 0; i < prefix.length(); i++) {
      if (prefix.charAt(i) == ChatColor.COLOR_CHAR) {
        isColor = true;
        continue;
      }

      if (isColor) {
        color = TextFormatter.convert(ChatColor.getByChar(prefix.charAt(i)));
        isColor = false;
      } else {
        prefixComponent.append(
            String.valueOf(prefix.charAt(i)), color != null ? color : TextColor.WHITE);
      }
    }

    return prefixComponent.build();
  }

  // Format component to have teleport click/hover
  static Component formatTeleport(Component username, String name) {
    return username
        .hoverEvent(
            HoverEvent.of(
                Action.SHOW_TEXT,
                TranslatableComponent.of("misc.teleportTo", TextColor.GRAY, username)))
        .clickEvent(ClickEvent.runCommand("/tp " + name));
  }

  // Player state checks
  static boolean isVanished(Player player) {
    return player.hasMetadata("isVanished");
  }

  static boolean isDead(Player player) {
    return player.hasMetadata("isDead") || player.isDead();
  }
}
