package tc.oc.pgm.modes;

import org.bukkit.Material;

public class ModeUtils {
  public static String formatMaterial(Material m) {
    switch (m) {
      case GOLD_BLOCK:
        return "GOLD";
      default:
        return m.name().replaceAll("_", " ");
    }
  }
}
