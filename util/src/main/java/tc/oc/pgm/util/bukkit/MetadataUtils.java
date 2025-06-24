package tc.oc.pgm.util.bukkit;

import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.MetadataValueAdapter;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.UnknownNullability;

public interface MetadataUtils {

  @SuppressWarnings("unchecked")
  static <T> @UnknownNullability T getMetadataValue(
      Metadatable metadatable, String key, Plugin plugin) {
    for (MetadataValue mv : metadatable.getMetadata(key)) {
      if (mv.getOwningPlugin().equals(plugin)) {
        return (T) mv.value();
      }
    }
    return null;
  }

  /** Create an immutable metadata value. Has less overhead than bukkit's FixedMetadataValue. */
  static MetadataValue createMetadataValue(Plugin plugin, Object value) {
    class ImmutableMetadataValue extends MetadataValueAdapter {
      private final Object value;

      public ImmutableMetadataValue(Plugin owningPlugin, Object value) {
        super(owningPlugin);
        this.value = value;
      }

      @Override
      public Object value() {
        return value;
      }

      @Override
      public void invalidate() {}
    }

    return new ImmutableMetadataValue(plugin, value);
  }
}
