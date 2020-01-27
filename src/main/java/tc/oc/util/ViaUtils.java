package tc.oc.util;

import org.bukkit.entity.Player;
import tc.oc.world.NMSHacks;
import us.myles.ViaVersion.api.Via;

public class ViaUtils {
  /**
   * Minecraft 1.7.6 &ndash; 1.7.10
   */
  public static final int VERSION_1_7 = 5;
  /**
   * Minecraft 1.8 &ndash; 1.8.9
   */
  public static final int VERSION_1_8 = 47;

  private static final boolean ENABLED;

  static {
    boolean viaLoaded = false;
    try {
      viaLoaded = Class.forName("us.myles.ViaVersion.api.Via") != null;
    } catch (ClassNotFoundException ignored) {
    }
    ENABLED = viaLoaded;
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
      return Via.getAPI().getPlayerVersion(player);
    } else {
      return NMSHacks.getProtocolVersion(player);
    }
  }
}
