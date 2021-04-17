package tc.oc.pgm.util.bukkit;

import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;

public interface MetadataUtils {
  static MetadataValue getMetadata(Metadatable metadatable, String key, Plugin plugin) {
    for (MetadataValue mv : metadatable.getMetadata(key)) {
      if (mv.getOwningPlugin().equals(plugin)) {
        return mv;
      }
    }
    return null;
  }
}
