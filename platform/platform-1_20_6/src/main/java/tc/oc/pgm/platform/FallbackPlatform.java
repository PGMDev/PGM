package tc.oc.pgm.platform;

import static tc.oc.pgm.util.platform.Supports.Priority.LOWEST;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;
import static tc.oc.pgm.util.platform.Supports.Variant.SPIGOT;

import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = SPIGOT, priority = LOWEST)
@Supports(value = PAPER, priority = LOWEST)
public class FallbackPlatform implements Platform.Manifest {
  @Override
  public void onEnable(Plugin plugin) {}
}
