package tc.oc.pgm.platform.modern;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGHEST;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.platform.modern.packets.PacketManipulations;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6", priority = HIGHEST)
public class ModernPlatform implements Platform.Manifest {
  @Override
  public void onEnable(Plugin plugin) {
    if (!plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
      Bukkit.getServer().getPluginManager().disablePlugin(plugin);
      throw new IllegalStateException(
          "ProtocolLib is not installed, and is required for PGM modern version support");
    }

    Bukkit.getServer().getPluginManager().registerEvents(new ModernListener(), plugin);
    Bukkit.getServer().getPluginManager().registerEvents(new SpawnEggUseListener(), plugin);

    PacketManipulations.registerAdapters(plugin);
  }
}
