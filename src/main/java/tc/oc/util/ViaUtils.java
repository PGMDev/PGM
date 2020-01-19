package tc.oc.util;

import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.world.NMSHacks;

public class ViaUtils {
  private static boolean enabled;
  private static Object viaAPI;
  private static Method getPlayerVersion;

  static {
    try {
      viaAPI = Class.forName("us.myles.ViaVersion.api.Via").getMethod("getAPI").invoke(null);
      getPlayerVersion =
          Class.forName("us.myles.ViaVersion.api.ViaAPI")
              .getMethod("getPlayerVersion", Object.class);
      enabled = true;
    } catch (Exception e) {
      PGM.get().getLogger().warning("ViaVersion is not installed");
      e.printStackTrace();
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
    int version = NMSHacks.getProtocolVersion(player);
    if (enabled()) {
      try {
        return (int) getPlayerVersion.invoke(viaAPI, player);
      } catch (Exception e) {
        e.printStackTrace();
        return version;
      }
    } else {
      return version;
    }
  }
}
