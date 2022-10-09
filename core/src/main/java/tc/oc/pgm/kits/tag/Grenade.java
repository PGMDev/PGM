package tc.oc.pgm.kits.tag;

import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.bukkit.MetadataUtils;
import tc.oc.pgm.util.inventory.tag.ItemTag;

public class Grenade {
  public final float power;
  public final boolean fire;
  public final boolean destroy;

  public Grenade(float power, boolean fire, boolean destroy) {
    this.power = power;
    this.fire = fire;
    this.destroy = destroy;
  }

  private static class Tag implements ItemTag<Grenade> {

    private static final ItemTag<String> TAG = ItemTag.newString("grenade");

    @Nullable
    @Override
    public Grenade get(ItemStack item) {
      String raw = TAG.get(item);
      if (raw == null) return null;

      String[] data = raw.split("-", 3);
      if (data.length != 3) return null;

      try {
        return new Grenade(Float.parseFloat(data[0]), data[1].equals("1"), data[2].equals("1"));
      } catch (NumberFormatException e) {
        return null;
      }
    }

    @Override
    public void set(ItemStack item, Grenade grenade) {
      TAG.set(
          item,
          String.format(
              "%.2f-%s-%s", grenade.power, grenade.fire ? "1" : "0", grenade.destroy ? "1" : "0"));
    }

    @Override
    public void clear(ItemStack item) {
      TAG.clear(item);
    }
  }

  public static final ItemTag<Grenade> ITEM_TAG = new Tag();

  private static final String METADATA_KEY = "grenade";

  public static boolean is(Metadatable entity) {
    return entity.hasMetadata(METADATA_KEY);
  }

  public static @Nullable Grenade get(Metadatable entity) {
    return entity.hasMetadata(METADATA_KEY)
        ? (Grenade) MetadataUtils.getMetadata(entity, METADATA_KEY, PGM.get()).value()
        : null;
  }

  public void set(Plugin plugin, Metadatable entity) {
    entity.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, this));
  }
}
