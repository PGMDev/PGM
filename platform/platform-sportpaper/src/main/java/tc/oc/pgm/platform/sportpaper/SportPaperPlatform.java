package tc.oc.pgm.platform.sportpaper;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGHEST;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.listeners.BlockPhysicsListener;
import tc.oc.pgm.listeners.BlockTransformListener;
import tc.oc.pgm.platform.sportpaper.material.ModernMaterialNames;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = SPORTPAPER, priority = HIGHEST)
public class SportPaperPlatform implements Platform.Manifest {

  @Override
  public void init() {
    ModernMaterialNames.validate();
  }

  @Override
  public void onEnable(Plugin plugin) {
    List.of(new SportPaperListener(), new BlockPhysicsListener())
        .forEach(l -> Bukkit.getPluginManager().registerEvents(l, plugin));

    new BlockTransformListener(plugin).registerEvents();
  }
}
