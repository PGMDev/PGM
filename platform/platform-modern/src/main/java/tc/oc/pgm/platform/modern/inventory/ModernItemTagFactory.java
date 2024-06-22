package tc.oc.pgm.platform.modern.inventory;

import org.bukkit.persistence.PersistentDataType;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = Supports.Variant.PAPER, minVersion = "1.20.6")
public class ModernItemTagFactory implements ItemTag.Factory {

  @Override
  public ItemTag<String> newString(String key) {
    return new ModernItemTag<>(key, PersistentDataType.STRING);
  }

  @Override
  public ItemTag<Boolean> newBoolean(String key) {
    return new ModernItemTag<>(key, PersistentDataType.BOOLEAN);
  }
}
