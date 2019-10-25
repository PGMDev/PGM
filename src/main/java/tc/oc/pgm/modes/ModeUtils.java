package tc.oc.pgm.modes;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;

public class ModeUtils {
  public static String formatMaterial(Material m) {
    switch (m) {
      case GOLD_BLOCK:
        return "GOLD";
      default:
        return m.name().replaceAll("_", " ");
    }
  }

  public static String formatMaterial(MaterialData m) {
    return formatMaterial(m.getItemType());
  }
}
