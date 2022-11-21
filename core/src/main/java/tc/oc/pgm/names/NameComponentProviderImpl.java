package tc.oc.pgm.names;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.named.NameDecorationProvider;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.named.NameStyle.Flag;
import tc.oc.pgm.util.text.PlayerComponentProvider;

public class NameComponentProviderImpl implements PlayerComponentProvider {

  public NameComponentProviderImpl() {}

  public String getPlayerName(Player player, String def) {
    return player != null ? player.getName() : def;
  }

  public String getVisibleName(Player viewer, Player player, String username) {
    if (viewer == null || player == null) return username;
    if (viewer != null && player != null && player.hasPermission(Permissions.ADMIN)) {
      if (viewer.hasPermission(Permissions.ADMIN)) {
        return player.getName();
      }
      return username;
    }

    boolean canViewRealName =
        viewer == player
            || Integration.isFriend(player, viewer)
            || viewer.hasPermission(Permissions.STAFF);
    return canViewRealName ? player.getName() : username;
  }

  @Override
  public Component renderName(Player player, String defName, NameStyle style, Pointered context) {

    Optional<UUID> audienceId = context.get(Identity.UUID);

    if (player == null && defName == null) {
      return UNKNOWN;
    }

    Player viewer = Bukkit.getPlayer(audienceId.orElse(null));

    NameDecorationProvider provider = NameDecorationProvider.DEFAULT;
    if (player != null) {
      MetadataValue metadata =
          player.getMetadata(NameDecorationProvider.METADATA_KEY, BukkitUtils.getPlugin());
      if (metadata != null) provider = (NameDecorationProvider) metadata.value();
    }

    boolean isOffline =
        player == null
            || !player.isOnline()
            || ((isDisguised(player) || Integration.getNick(player) != null)
                && style.has(NameStyle.Flag.DISGUISE_OFFLINE));

    UUID uuid = !isOffline ? player.getUniqueId() : null;

    boolean isNicked = uuid != null && Integration.getNick(player) != null;
    String username = isNicked ? Integration.getNick(player) : getPlayerName(player, defName);

    TextComponent.Builder builder = text();
    if (!isOffline && style.has(NameStyle.Flag.FLAIR)) {
      if (!isNicked || canViewNick(player, viewer)) {
        builder.append(provider.getPrefixComponent(uuid));
      }
    }

    TextComponent.Builder name = text().content(getVisibleName(viewer, player, username));

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
    }

    if (!isOffline
        && style.has(Flag.REVEAL)
        && isNicked
        && viewer != null
        && canViewNick(player, viewer)) {
      name.decoration(TextDecoration.STRIKETHROUGH, true);
      name.append(space().decoration(TextDecoration.STRIKETHROUGH, false))
          .append(
              text(
                      Integration.getNick(player),
                      provider.getColor(player.getUniqueId()),
                      TextDecoration.ITALIC)
                  .decoration(TextDecoration.STRIKETHROUGH, false));
    }

    if (!isOffline
        && style.has(NameStyle.Flag.FRIEND)
        && viewer != null
        && (!isNicked || canViewNick(player, viewer))
        && Integration.isFriend(viewer, player)) {
      name.decoration(TextDecoration.ITALIC, true);
    }

    if (!isOffline && style.has(NameStyle.Flag.TELEPORT)) {
      name.hoverEvent(
              HoverEvent.showText(
                  translatable("misc.teleportTo", NamedTextColor.GRAY, name.build())))
          .clickEvent(
              ClickEvent.runCommand(
                  "/tp "
                      + (Integration.getNick(player) != null
                          ? Integration.getNick(player)
                          : player.getName())));
    }

    builder.append(name);

    if (!isOffline && style.has(NameStyle.Flag.FLAIR) && canViewNick(player, viewer)) {
      builder.append(provider.getSuffixComponent(uuid));
    }

    return builder.build();
  }

  // Player state checks
  static boolean isDisguised(Player player) {
    return Integration.isVanished(player) || Integration.isHidden(player);
  }

  static boolean isDead(Player player) {
    return player.hasMetadata("isDead") || player.isDead();
  }

  static boolean canViewNick(Player player, @Nullable Player viewer) {
    if (viewer == null) return false;
    if (player.hasPermission(Permissions.ADMIN)) return viewer.hasPermission(Permissions.ADMIN);
    return viewer == player
        || viewer.hasPermission(Permissions.STAFF)
        || Integration.isFriend(player, viewer);
  }
}
