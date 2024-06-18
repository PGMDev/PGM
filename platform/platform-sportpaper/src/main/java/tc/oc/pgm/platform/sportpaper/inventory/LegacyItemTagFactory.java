package tc.oc.pgm.platform.sportpaper.inventory;

import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.platform.Supports;

@Supports(Supports.Variant.SPORTPAPER)
public class LegacyItemTagFactory implements ItemTag.Factory {

  @Override
  public ItemTag<String> newString(String key) {
    return new LegacyItemTag();
  }

  @Override
  public ItemTag<Boolean> newBoolean(String key) {
    return new LegacyBooleanItemTag(newString(key));
  }
}
