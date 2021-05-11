package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.platform.AudienceIdentity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.xml.XMLUtils;

public interface PlayerComponentProvider {

  public static final TextColor OFFLINE_COLOR = NamedTextColor.DARK_AQUA;
  public static final Component UNKNOWN =
      translatable("misc.unknown", OFFLINE_COLOR, TextDecoration.ITALIC);
  public static final Component CONSOLE =
      translatable("misc.console", OFFLINE_COLOR, TextDecoration.ITALIC);

  static AtomicReference<PlayerComponentProvider> PROVIDER =
      new AtomicReference<PlayerComponentProvider>(new NoopNameComponentProvider());

  static Component render(String id, NameStyle nameStyle, AudienceIdentity context) {
    if (id == null) return CONSOLE;

    if (XMLUtils.USERNAME_REGEX.matcher(id).matches()) {
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

  Component renderName(Player player, String defName, NameStyle style, AudienceIdentity context);

  public static class NoopNameComponentProvider implements PlayerComponentProvider {
    @Override
    public Component renderName(
        Player player, String defName, NameStyle style, AudienceIdentity context) {
      return text(player != null ? player.getName() : defName);
    }
  }
}
