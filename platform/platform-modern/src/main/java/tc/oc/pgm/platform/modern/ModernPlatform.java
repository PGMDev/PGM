package tc.oc.pgm.platform.modern;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGHEST;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.spigotmc.SpigotConfig;
import tc.oc.pgm.platform.modern.listeners.DispenserListener;
import tc.oc.pgm.platform.modern.listeners.ModernBlockPhysicsListener;
import tc.oc.pgm.platform.modern.listeners.ModernBlockTransformListener;
import tc.oc.pgm.platform.modern.listeners.ModernListener;
import tc.oc.pgm.platform.modern.listeners.PlayerTracker;
import tc.oc.pgm.platform.modern.listeners.RecipeUnlocker;
import tc.oc.pgm.platform.modern.listeners.SpawnEggUseListener;
import tc.oc.pgm.platform.modern.listeners.TntListener;
import tc.oc.pgm.platform.modern.packets.PacketManipulations;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "26.2", priority = HIGHEST)
public class ModernPlatform implements Platform.Manifest {
  private PacketManipulations packetManipulations;

  @Override
  public void onEnable(Plugin plugin) {
    if (!plugin.getServer().getPluginManager().isPluginEnabled("packetevents")) {
      Bukkit.getServer().getPluginManager().disablePlugin(plugin);
      throw new IllegalStateException(
          "PacketEvents is not installed, and is required for PGM modern version support");
    }

    PlayerTracker tracker;
    List.of(
            new DispenserListener(),
            new ModernListener(),
            tracker = new PlayerTracker(),
            new RecipeUnlocker(),
            new SpawnEggUseListener(),
            new TntListener(),
            new ModernBlockPhysicsListener())
        .forEach(l -> Bukkit.getServer().getPluginManager().registerEvents(l, plugin));

    new ModernBlockTransformListener(plugin).registerEvents();

    packetManipulations = new PacketManipulations(tracker);

    if (!SpigotConfig.disabledAdvancements.contains("*")) {
      plugin.getLogger().warning("""
              You have not disabled advancements in your spigot config.
              If you want to remove them you should modify your spigot.yml config to have:
              advancements:
                disable-saving: true
                disabled:
                - '*'
              """);
    }
  }

  @Override
  public void onDisable() {
    if (packetManipulations != null) {
      packetManipulations.unregister();
      packetManipulations = null;
    }
  }
}
