package tc.oc.pgm.util.bukkit;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import com.viaversion.viaversion.api.Via;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.platform.Platform;

public class ViaUtils {
  public static final int VERSION_1_7 = 5;
  public static final int VERSION_1_8 = 47;
  public static final int VERSION_1_13 = 393;
  public static final int VERSION_1_21_11 = 774;

  private static final boolean ENABLED = isViaLoaded();

  private static boolean isViaLoaded() {
    try {
      Class.forName("com.viaversion.viaversion.api.Via");
      return true;
    } catch (ClassNotFoundException ignored) {
      return false;
    }
  }

  public static boolean enabled() {
    return ENABLED;
  }

  /**
   * @see <a
   *     href="https://minecraft.wiki/w/Java_Edition_data_values/Protocol_and_data_versions">https://minecraft.wiki/w/Java_Edition_data_values/Protocol_and_data_versions</a>
   */
  public static int getProtocolVersion(Player player) {
    if (enabled()) {
      return Via.getAPI().getPlayerVersion(player.getUniqueId());
    } else {
      return Platform.VARIANT == SPORTPAPER ? VERSION_1_8 : VERSION_1_21_11;
    }
  }

  public static boolean isReady(Player player) {
    return !enabled() || Via.getAPI().isInjected(player.getUniqueId());
  }
}
