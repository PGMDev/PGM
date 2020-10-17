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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.named.NameDecorationProvider;
import tc.oc.pgm.util.named.NameStyle;

/** PlayerComponent is used to format player names in a consistent manner with optional styling */
public interface PlayerComponent {

  TextColor OFFLINE_COLOR = TextColor.DARK_AQUA;
  static Component UNKNOWN =
      TranslatableComponent.of("misc.unknown", OFFLINE_COLOR, TextDecoration.ITALIC);

  static Component of(UUID playerId, NameStyle style) {
    Player player = Bukkit.getPlayer(playerId);
    return player != null ? of(player, style) : UNKNOWN;
  }

  static Component of(CommandSender sender, NameStyle style) {
    return sender instanceof Player
        ? of((Player) sender, style)
        : TranslatableComponent.of("misc.console", OFFLINE_COLOR);
  }

  static Component of(Player player, NameStyle style) {
    return of(player, style, null);
  }

  static Component of(Player player, String defName, NameStyle style) {
    return of(player, defName, style, null);
  }

  static Component of(Player player, NameStyle style, @Nullable Player viewer) {
    return of(player, "", style, viewer);
  }

  static Component of(
      @Nullable Player player, String defName, NameStyle style, @Nullable Player viewer) {
    boolean isOffline =
        player == null
            || !player.isOnline()
            || (isDisguised(player) && style.has(NameStyle.Flag.DISGUISE_OFFLINE));

    NameDecorationProvider provider = NameDecorationProvider.DEFAULT;
    if (player != null) {
      MetadataValue metadata =
          player.getMetadata(NameDecorationProvider.METADATA_KEY, BukkitUtils.getPlugin());
      if (metadata != null) provider = (NameDecorationProvider) metadata.value();
    }

    UUID uuid = !isOffline ? player.getUniqueId() : null;

    TextComponent.Builder builder = TextComponent.builder();
    if (!isOffline && style.has(NameStyle.Flag.FLAIR)) {
      builder.append(provider.getPrefixComponent(uuid));
    }

    TextComponent.Builder name = TextComponent.builder(player != null ? player.getName() : defName);

    if (!isOffline && style.has(NameStyle.Flag.DEATH) && isDead(player)) {
      name.color(TextColor.GRAY);
    } else if (style.has(NameStyle.Flag.COLOR)) {
      name.color(isOffline ? OFFLINE_COLOR : provider.getColor(uuid));
    }
    if (!isOffline && style.has(NameStyle.Flag.SELF) && player == viewer) {
      name.decoration(TextDecoration.BOLD, true);
    }
    if (!isOffline && style.has(NameStyle.Flag.DISGUISE) && isDisguised(player)) {
      name.decoration(TextDecoration.STRIKETHROUGH, true);
    }
    if (!isOffline && style.has(NameStyle.Flag.TELEPORT)) {
      name.hoverEvent(
              HoverEvent.of(
                  Action.SHOW_TEXT,
                  TranslatableComponent.of("misc.teleportTo", TextColor.GRAY, name.build())))
          .clickEvent(ClickEvent.runCommand("/tp " + player.getName()));
    }

    builder.append(name);

    if (style.has(NameStyle.Flag.FLAIR) && !isOffline) {
      builder.append(provider.getSuffixComponent(uuid));
    }
    return builder.build();
  }

  // Player state checks
  static boolean isDisguised(Player player) {
    return player.hasMetadata("isVanished");
  }

  static boolean isDead(Player player) {
    return player.hasMetadata("isDead") || player.isDead();
  }
}
