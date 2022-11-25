package tc.oc.pgm.util.bukkit;

import java.util.Optional;
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

  static Optional<MetadataValue> getOptionalMetadata(
      Metadatable metadatable, String key, Plugin plugin) {
    return Optional.ofNullable(getMetadata(metadatable, key, plugin));
  }
}
