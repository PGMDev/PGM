package tc.oc.pgm.util.bukkit;

import com.viaversion.viaversion.api.Via;
import org.bukkit.entity.Player;

public class ViaUtils {
  /** Minecraft 1.7.6 &ndash; 1.7.10 */
  public static final int VERSION_1_7 = 5;
  /** Minecraft 1.8 &ndash; 1.8.9 */
  public static final int VERSION_1_8 = 47;
  /** Minecraft 1.9 &ndash; 1.9.1-pre1 */
  public static final int VERSION_1_9 = 107;

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
   *     href="https://wiki.vg/Protocol_version_numbers">https://wiki.vg/Protocol_version_numbers</a>
   */
  public static int getProtocolVersion(Player player) {
    if (enabled()) {
      return Via.getAPI().getPlayerVersion(player.getUniqueId());
    } else {
      return VERSION_1_8;
    }
  }

  public static boolean isReady(Player player) {
    return !enabled() || Via.getAPI().isInjected(player.getUniqueId());
  }
}
