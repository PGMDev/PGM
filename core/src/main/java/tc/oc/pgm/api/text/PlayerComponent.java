package tc.oc.pgm.api.text;

import static net.kyori.adventure.text.Component.space;
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
import tc.oc.pgm.util.bukkit.MetadataUtils;
import org.bukkit.metadata.MetadataValue;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.named.NameDecorationProvider;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.named.NameStyle.Flag;

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

  public static Component player(UUID playerId, String defName, NameStyle style) {
    Player player = Bukkit.getPlayer(playerId);
    return player != null
        ? player(player, style)
        : defName != null ? player(null, defName, style, null) : UNKNOWN;
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

    NameDecorationProvider provider = NameDecorationProvider.DEFAULT;
    if (player != null && player.hasMetadata(NameDecorationProvider.METADATA_KEY)) {
      provider =
          (NameDecorationProvider)
              MetadataUtils.getMetadata(
                      player, NameDecorationProvider.METADATA_KEY, BukkitUtils.getPlugin())
                  .value();
    }

    boolean isOffline =
        player == null
            || !player.isOnline()
            || ((isDisguised(player) || Integration.getNick(player) != null)
                && style.has(NameStyle.Flag.DISGUISE_OFFLINE));

    UUID uuid = !isOffline ? player.getUniqueId() : null;
    boolean isNicked = uuid != null && Integration.getNick(player) != null;
    String nicked = !isOffline && player != null ? Integration.getNick(player) : defName;

    TextComponent.Builder builder = text();
    if (!isOffline && style.has(NameStyle.Flag.FLAIR)) {
      if (!isNicked || canViewNick(player, viewer)) {
        builder.append(provider.getPrefixComponent(uuid));
      }
    }

    TextComponent.Builder name = text().content(getName(player, viewer, nicked));

    if (!isOffline && style.has(NameStyle.Flag.DEATH) && isDead(player)) {
      name.color(NamedTextColor.DARK_GRAY);
    } else if (style.has(NameStyle.Flag.COLOR)) {
      name.color(isOffline ? OFFLINE_COLOR : provider.getColor(uuid));
    }
    if (!isOffline && style.has(NameStyle.Flag.SELF) && player == viewer) {
      name.decoration(TextDecoration.BOLD, true);
    }
    if (!isOffline
        && style.has(NameStyle.Flag.DISGUISE)
        && (isDisguised(player) || (isNicked && canViewNick(player, viewer)))) {

      name.decoration(TextDecoration.STRIKETHROUGH, true);

      // Reveal nickname
      if (isNicked && style.has(Flag.REVEAL) && viewer != null && canViewNick(player, viewer)) {
        name.append(space().decoration(TextDecoration.STRIKETHROUGH, false))
            .append(
                text(
                        Integration.getNick(player),
                        provider.getColor(player.getUniqueId()),
                        TextDecoration.ITALIC)
                    .decoration(TextDecoration.STRIKETHROUGH, false));
      }
    }

    if (!isOffline
        && style.has(NameStyle.Flag.FRIEND)
        && viewer != null
        && Integration.isFriend(viewer, player)) {
      name.decoration(TextDecoration.ITALIC, true);
    }

    if (!isOffline && style.has(NameStyle.Flag.TELEPORT)) {
      name.hoverEvent(showText(translatable("misc.teleportTo", NamedTextColor.GRAY, name.build())))
          .clickEvent(runCommand("/tp " + player.getName()));
    }

    builder.append(name);

    if (!isOffline && style.has(NameStyle.Flag.FLAIR) && canViewNick(player, viewer)) {
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

  static boolean canViewNick(Player player, @Nullable Player viewer) {
    if (viewer == null) return false;
    if (viewer == player) return true;
    return viewer.hasPermission("pgm.staff") || Integration.isFriend(player, viewer);
  }

  static String getName(@Nullable Player player, @Nullable Player viewer, String defName) {
    if (player != null
        && viewer != null
        && (viewer == player
            || Integration.isFriend(player, viewer)
            || viewer.hasPermission("pgm.staff"))) {
      return player.getName();
    }
    return defName;
  }
}
