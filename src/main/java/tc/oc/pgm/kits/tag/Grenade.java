package tc.oc.pgm.kits.tag;

import javax.annotation.Nullable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;

public class Grenade {
  public final float power;
  public final boolean fire;
  public final boolean destroy;

  public Grenade(float power, boolean fire, boolean destroy) {
    this.power = power;
    this.fire = fire;
    this.destroy = destroy;
  }

  public static final GrenadeItemTag ITEM_TAG = new GrenadeItemTag();

  private static final String METADATA_KEY = "grenade";

  public static boolean is(Metadatable entity) {
    return entity.hasMetadata(METADATA_KEY);
  }

  public static @Nullable Grenade get(Metadatable entity) {
    return entity.hasMetadata(METADATA_KEY)
        ? (Grenade) entity.getMetadata(METADATA_KEY).get(0).value()
        : null;
  }

  public void set(Plugin plugin, Metadatable entity) {
    entity.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, this));
  }
}
