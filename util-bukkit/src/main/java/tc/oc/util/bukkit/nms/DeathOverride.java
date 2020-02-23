package tc.oc.util.bukkit.nms;

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

/**
 * A somewhat hacky mechanism for faking an entity's death. The name rendering system checks this
 * when deciding if a player's name should be greyed out, and PGM sets it.
 */
public interface DeathOverride {

  String METADATA_KEY = "isDead";
  AtomicReference<Plugin> PLUGIN_KEY = new AtomicReference<>();

  static Plugin getKey() {
    if (PLUGIN_KEY.get() == null) {
      // FIXME: util should not be accessing PGM
      PLUGIN_KEY.set(Bukkit.getPluginManager().getPlugin("PGM"));
    }
    return PLUGIN_KEY.get();
  }

  /**
   * Set or clear a metadata flag on the given entity that overrides their default alive/dead
   * status.
   */
  static void setDead(Entity player, @Nullable Boolean dead) {
    if (dead != null) {
      player.setMetadata(METADATA_KEY, new FixedMetadataValue(getKey(), dead));
    } else {
      player.removeMetadata(METADATA_KEY, getKey());
    }
  }

  /**
   * Test if the given entity is dead, first by checking their metadata for an overridden value, and
   * falling back to {@link Entity#isDead}.
   */
  static boolean isDead(Entity player) {
    MetadataValue value = player.getMetadata(METADATA_KEY, getKey());
    if (value != null) {
      return value.asBoolean();
    } else {
      return player.isDead();
    }
  }
}
