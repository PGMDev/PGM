package tc.oc.pgm.util.bukkit;

import org.bukkit.entity.Player;
import tc.oc.pgm.util.nms.NMSHacks;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.boss.BossBar;
import us.myles.ViaVersion.api.boss.BossColor;
import us.myles.ViaVersion.api.boss.BossStyle;

public class ViaUtils {
  /** Minecraft 1.7.6 &ndash; 1.7.10 */
  public static final int VERSION_1_7 = 5;
  /** Minecraft 1.8 &ndash; 1.8.9 */
  public static final int VERSION_1_8 = 47;
  /** Minecraft 1.9 &ndash; 1.9.1-pre1 */
  public static final int VERSION_1_9 = 107;

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
      return Via.getAPI().getPlayerVersion(player.getUniqueId());
    } else {
      return NMSHacks.getProtocolVersion(player);
    }
  }

  public static boolean isReady(Player player) {
    return !enabled() || Via.getAPI().isInjected(player.getUniqueId());
  }

  public static BossBar<?> createBossBar() {
    return enabled() ? Via.getAPI().createBossBar("", BossColor.BLUE, BossStyle.SOLID) : null;
  }
}
