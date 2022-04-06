package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.named.NameDecorationProvider;
import tc.oc.pgm.api.named.NameStyle;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.MetadataUtils;

/** PlayerComponent is used to format player names in a consistent manner with optional styling */
public final class PlayerComponent {
  private PlayerComponent() {}

  public static final TextColor OFFLINE_COLOR = NamedTextColor.DARK_AQUA;
  public static final Component UNKNOWN =
      translatable("misc.unknown", OFFLINE_COLOR, TextDecoration.ITALIC);

  public static Component player(UUID playerId, NameStyle style) {
    Player player = Bukkit.getPlayer(playerId);
    return player != null ? player(player, style) : UNKNOWN;
  }

  public static Component player(CommandSender sender, NameStyle style) {
    return sender instanceof Player
        ? player((Player) sender, style)
        : translatable("misc.console", OFFLINE_COLOR);
  }

  public static Component player(Player player, NameStyle style) {
    return player(player, style, null);
  }

  public static Component player(Player player, String defName, NameStyle style) {
    return player(player, defName, style, null);
  }

  public static Component player(Player player, NameStyle style, @Nullable Player viewer) {
    return player(player, "", style, viewer);
  }

  public static Component player(
      @Nullable Player player, String defName, NameStyle style, @Nullable Player viewer) {
    boolean isOffline =
        player == null
            || !player.isOnline()
            || (isDisguised(player) && style.has(NameStyle.Flag.DISGUISE_OFFLINE));

    NameDecorationProvider provider = NameDecorationProvider.DEFAULT;
    if (player != null && player.hasMetadata(NameDecorationProvider.METADATA_KEY)) {
      provider =
          (NameDecorationProvider)
              MetadataUtils.getMetadata(
                      player, NameDecorationProvider.METADATA_KEY, BukkitUtils.getPlugin())
                  .value();
    }

    UUID uuid = !isOffline ? player.getUniqueId() : null;

    TextComponent.Builder builder = text();
    if (!isOffline && style.has(NameStyle.Flag.FLAIR)) {
      builder.append(provider.getPrefixComponent(uuid));
    }

    TextComponent.Builder name = text().content(player != null ? player.getName() : defName);

    if (!isOffline && style.has(NameStyle.Flag.DEATH) && isDead(player)) {
      name.color(NamedTextColor.DARK_GRAY);
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
      name.hoverEvent(showText(translatable("misc.teleportTo", NamedTextColor.GRAY, name.build())))
          .clickEvent(runCommand("/tp " + player.getName()));
    }

    builder.append(name);

    if (style.has(NameStyle.Flag.FLAIR) && !isOffline) {
      builder.append(provider.getSuffixComponent(uuid));
    }

    if (builder.children().size() == 1) return name.build();
    return builder.build();
  }

  // Player state checks
  public static boolean isDisguised(Player player) {
    return player.hasMetadata("isVanished");
  }

  public static boolean isDead(Player player) {
    return player.hasMetadata("isDead") || player.isDead();
  }
}
