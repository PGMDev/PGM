package tc.oc.pgm.modes;

import org.bukkit.Material;
import tc.oc.pgm.util.material.MaterialData;

public class ModeUtils {
  public static String formatMaterial(Material m) {
    return switch (m) {
      case GOLD_BLOCK -> "GOLD";
      default -> m.name().replaceAll("_", " ");
    };
  }

  public static String formatMaterial(MaterialData m) {
    if (m == null) return "Unknown mode";
    return formatMaterial(m.getItemType());
  }
}
