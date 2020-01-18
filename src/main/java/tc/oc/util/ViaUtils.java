package tc.oc.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.world.NMSHacks;
import us.myles.ViaVersion.api.Via;

public class ViaUtils {
  public static boolean enabled() {
    return Bukkit.getServer().getPluginManager().getPlugin("ViaVersion") != null;
  }

  public static int getProtocolVersion(Player player) {
    return enabled() ? Via.getAPI().getPlayerVersion(player) : NMSHacks.getProtocolVersion(player);
  }
}
