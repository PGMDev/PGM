package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;

public interface PlayerComponentProvider {

  public static final Pattern USERNAME_REGEX = Pattern.compile("[a-zA-Z0-9_]{1,16}");

  public static final TextColor OFFLINE_COLOR = NamedTextColor.DARK_AQUA;
  public static final Component UNKNOWN =
      translatable("misc.unknown", OFFLINE_COLOR, TextDecoration.ITALIC);
  public static final Component CONSOLE =
      translatable("misc.console", OFFLINE_COLOR, TextDecoration.ITALIC);

  static AtomicReference<PlayerComponentProvider> PROVIDER =
      new AtomicReference<PlayerComponentProvider>(new NoopNameComponentProvider());

  static Component render(String id, NameStyle nameStyle, Pointered context) {
    if (id == null) return CONSOLE;

    if (USERNAME_REGEX.matcher(id).matches()) {
      String name = id.equalsIgnoreCase("null") ? null : id;
      return PROVIDER.get().renderName(null, name, nameStyle, context);
    } else {
      try {
        UUID playerId = UUID.fromString(id);
        Player player = Bukkit.getPlayer(playerId);
        return PROVIDER.get().renderName(player, null, nameStyle, context);
      } catch (IllegalArgumentException e) {
        return empty();
      }
    }
  }

  Component renderName(Player player, String defName, NameStyle style, Pointered context);

  public static class NoopNameComponentProvider implements PlayerComponentProvider {
    @Override
    public Component renderName(Player player, String defName, NameStyle style, Pointered context) {
      if (player == null && defName == null) return UNKNOWN;
      return text(player != null ? player.getDisplayName() : defName);
    }
  }
}
