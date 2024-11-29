package tc.oc.pgm.platform.modern;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGHEST;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.spigotmc.SpigotConfig;
import tc.oc.pgm.platform.modern.packets.PacketManipulations;
import tc.oc.pgm.platform.modern.util.PlayerTracker;
import tc.oc.pgm.platform.modern.util.RecipeUnlocker;
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

    PlayerTracker tracker;
    List.of(
            new ModernListener(),
            new SpawnEggUseListener(),
            new RecipeUnlocker(),
            tracker = new PlayerTracker())
        .forEach(l -> Bukkit.getServer().getPluginManager().registerEvents(l, plugin));

    new PacketManipulations(plugin, tracker);

    if (!SpigotConfig.disabledAdvancements.contains("*")) {
      plugin
          .getLogger()
          .warning(
              """
              You have not disabled advancements in your spigot config.
              If you want to remove them you should modify your spigot.yml config to have:
              advancements:
                disable-saving: true
                disabled:
                - '*'
              """);
    }
  }
}
