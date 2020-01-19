package tc.oc.util;

import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.world.NMSHacks;
import us.myles.ViaVersion.api.Via;

public class ViaUtils {
  private static boolean enabled;

  static {
    try {
      enabled = Class.forName("us.myles.ViaVersion.api.Via") != null;
    } catch (Exception e) {
      PGM.get().getLogger().warning("ViaVersion is not installed");
      enabled = false;
    }
  }

  public static boolean enabled() {
    return enabled;
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
