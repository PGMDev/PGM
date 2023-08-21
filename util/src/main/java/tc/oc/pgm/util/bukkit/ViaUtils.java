package tc.oc.pgm.util.bukkit;

import com.viaversion.viaversion.api.Via;
import java.lang.reflect.Field;
import java.util.List;
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

  /**
   * Adventure has a ViaFacet$Chat class, which sends text using 1.16 format for support for hex
   * codes. The issue is by doing that, it skips all translation layers from 1.8 to 1.16, including
   * a needed rename for translated items to work. Removing this means hex colors would be
   * restricted to the 16 colors even for 1.16 clients (pgm doesn't use them) but translations will
   * be correct.
   */
  public static void removeViaChatFacet() {
    try {
      Class<?> bukkitAudience = Class.forName("net.kyori.adventure.platform.bukkit.BukkitAudience");
      Field f = bukkitAudience.getDeclaredField("CHAT");
      f.setAccessible(true);
      List<?> list = (List<?>) f.get(null);
      list.removeIf(el -> el.getClass().getName().endsWith("ViaFacet$Chat"));
    } catch (ReflectiveOperationException ignored) {
    }
  }
}
