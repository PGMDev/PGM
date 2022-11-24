package tc.oc.pgm.util;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.MetadataUtils;
import tc.oc.pgm.util.named.NameDecorationProvider;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.named.NameStyle.Flag;
import tc.oc.pgm.util.text.RenderableComponent;

/** PlayerComponent is used to format player names in a consistent manner with optional styling */
public final class PlayerComponent implements RenderableComponent {

  public static final TextColor OFFLINE_COLOR = NamedTextColor.DARK_AQUA;
  public static final TextColor DEAD_COLOR = NamedTextColor.DARK_GRAY;

  public static final Component UNKNOWN =
      translatable("misc.unknown", OFFLINE_COLOR, TextDecoration.ITALIC);
  public static final Component CONSOLE = translatable("misc.console", OFFLINE_COLOR);

  private final Player player;
  private final String defName;
  private final NameStyle style;

  private PlayerComponent(@Nullable Player player, String defName, NameStyle style) {
    this.player = player;
    this.defName = defName;
    this.style = style;
  }

  public static Component player(UUID playerId, NameStyle style) {
    Player player = Bukkit.getPlayer(playerId);
    return player != null ? player(player, style) : UNKNOWN;
  }

  public static Component player(CommandSender sender, NameStyle style) {
    return sender instanceof Player ? player((Player) sender, style) : CONSOLE;
  }

  public static PlayerComponent player(Player player, NameStyle style) {
    return player(player, null, style);
  }

  public static PlayerComponent player(Player player, String defName, NameStyle style) {
    return new PlayerComponent(player, defName, style);
  }

  public Component render(CommandSender viewer) {
    if (player == null && defName == null) {
      return UNKNOWN;
    }

    boolean online = player != null && player.isOnline();
    boolean reveal = online && Players.shouldReveal(viewer, player);

    String nick = player != null ? Integration.getNick(player) : null;

    NameDecorationProvider provider = getNameDecorations();

    UUID uuid = online ? player.getUniqueId() : null;

    TextComponent.Builder builder = text();
    if (online && reveal && style.has(Flag.FLAIR)) {
      builder.append(provider.getPrefixComponent(uuid));
    }

    String visibleName = player != null ? Players.getVisibleName(viewer, player) : defName;

    TextComponent.Builder name = text().content(visibleName);

    TextColor color = getColor(online, uuid, provider);
    if (color != null) name.color(color);

    if (player == viewer && reveal && style.has(Flag.SELF)) {
      name.decoration(TextDecoration.BOLD, true);
    }
    if (reveal && style.has(Flag.FRIEND) && Players.isFriend(viewer, player)) {
      name.decoration(TextDecoration.ITALIC, true);
    }
    if (reveal && style.has(Flag.DISGUISE) && (nick != null || Integration.isVanished(player))) {
      name.decoration(TextDecoration.STRIKETHROUGH, true);

      if (nick != null && style.has(Flag.NICKNAME)) {
        name.append(
            text(" " + nick, color, TextDecoration.ITALIC)
                .decoration(TextDecoration.STRIKETHROUGH, false));
      }
    }

    if (online && style.has(Flag.TELEPORT)) {
      name.hoverEvent(showText(translatable("misc.teleportTo", NamedTextColor.GRAY, name.build())))
          .clickEvent(runCommand("/tp " + visibleName));
    }

    builder.append(name);

    if (online && reveal && style.has(Flag.FLAIR)) {
      builder.append(provider.getSuffixComponent(uuid));
    }

    // Optimization: only if flairs were rendered do we need the whole builder.
    return online && reveal && style.has(Flag.FLAIR) ? builder.build() : name.build();
  }

  private TextColor getColor(boolean online, UUID uuid, NameDecorationProvider provider) {
    if (online && style.has(Flag.DEATH) && isDead()) {
      return DEAD_COLOR;
    } else if (style.has(Flag.COLOR)) {
      return online ? provider.getColor(uuid) : OFFLINE_COLOR;
    }
    return null;
  }

  private NameDecorationProvider getNameDecorations() {
    if (player != null && player.hasMetadata(NameDecorationProvider.METADATA_KEY)) {
      return MetadataUtils.getOptionalMetadata(
              player, NameDecorationProvider.METADATA_KEY, BukkitUtils.getPlugin())
          .map(mv -> (NameDecorationProvider) mv.value())
          .orElse(NameDecorationProvider.DEFAULT);
    }
    return NameDecorationProvider.DEFAULT;
  }

  public boolean isDead() {
    return player != null && (player.hasMetadata("isDead") || player.isDead());
  }
}
