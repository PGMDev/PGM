package tc.oc.pgm.platform;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;
import static tc.oc.pgm.util.platform.Supports.Variant.SPIGOT;

import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.MiscUtils;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER)
@Supports(value = SPIGOT)
public class NoOpMiscUtil implements MiscUtils {

  public NoOpMiscUtil() {
    BukkitUtils.getPlugin()
        .getLogger()
        .warning("Several misc utilities aren't supported in the current platform");
  }
}
