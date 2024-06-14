package tc.oc.pgm.platform.v1_20_6;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGHEST;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6", priority = HIGHEST)
public class ModernPlatform implements Platform.Manifest {
  @Override
  public void onEnable(Plugin plugin) {
    Bukkit.getServer().getPluginManager().registerEvents(new ModernListener(), plugin);

    MaterialMatcher matcher =
        MaterialMatcher.builder().add(m -> m.name().endsWith("_SPAWN_EGG")).build();
    Bukkit.getServer()
        .getPluginManager()
        .registerEvents(new PlayerPlaceEntityListener(matcher), plugin);
  }
}
