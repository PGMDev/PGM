package tc.oc.pgm.platform;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;
import static tc.oc.pgm.util.platform.Supports.Variant.SPIGOT;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = SPIGOT, minVersion = "1.9")
@Supports(value = PAPER, minVersion = "1.9")
public class ProtocolLibPlatform implements Platform.Manifest {
  @Override
  public void onEnable(Plugin plugin) {
    if (!Bukkit.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
      Bukkit.getServer().getPluginManager().disablePlugin(plugin);
      throw new UnsupportedOperationException(
          "ProtocolLib is required for PGM to run on this platform");
    }
  }
}
