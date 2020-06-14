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
    TextComponent.Builder component = TextComponent.builder();

    // Offline player
    if (player == null || !player.isOnline()) {
      TextComponent name = TextComponent.of(defName);
      if (style != NameStyle.PLAIN) name = name.color(TextColor.DARK_AQUA);
      component.append(name);
      return component.build();
    }

    // For name styles that don't allow vanished, make vanished appear offline
    if (!style.showVanish && isVanished(player)) {
      TextComponent name = TextComponent.of(player.getName());
      if (style != NameStyle.PLAIN) name = name.color(TextColor.DARK_AQUA);
      component.append(name);
      return component.build();
    }

    String realName = player.getName();
    String displayName = player.getDisplayName();
    String prefix = displayName.replace(realName, "");

    char colorChar = displayName.charAt((displayName.indexOf(realName) - 1));
    TextColor color =
        style != NameStyle.PLAIN
            ? TextFormatter.convert(ChatColor.getByChar(colorChar))
            : TextColor.WHITE;

    Component prefixComponent = TextComponent.of(prefix);
    Component usernameComponent = TextComponent.of(realName, color);

    if (!style.showPrefix) {
      prefixComponent = TextComponent.empty();
    }

    if (style.showDeath && isDead(player)) {
      usernameComponent = usernameComponent.color(TextColor.DARK_GRAY);
    }

    if (style.showVanish && isVanished(player)) {
      usernameComponent = usernameComponent.decoration(TextDecoration.STRIKETHROUGH, true);
    }

    Component formattedUsername =
        TextComponent.builder().append(prefixComponent).append(usernameComponent).build();

    component.append(formattedUsername);

    if (style.teleport) {
      component.hoverEvent(
          HoverEvent.of(
              Action.SHOW_TEXT,
              TranslatableComponent.of("misc.teleportTo", TextColor.GRAY).args(formattedUsername)));
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
