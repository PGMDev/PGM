package tc.oc.pgm.platform.sportpaper;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGHEST;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = SPORTPAPER, priority = HIGHEST)
public class SportPaperPlatform implements Platform.Manifest {
  @Override
  public void onEnable(Plugin plugin) {
    Bukkit.getServer().getPluginManager().registerEvents(new SportPaperListener(), plugin);
  }
}
