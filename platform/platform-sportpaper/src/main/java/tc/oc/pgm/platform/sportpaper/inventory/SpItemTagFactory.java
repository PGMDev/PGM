package tc.oc.pgm.platform.sportpaper.inventory;

import java.util.List;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.platform.Supports;

@Supports(Supports.Variant.SPORTPAPER)
public class SpItemTagFactory implements ItemTag.Factory {
  @Override
  public ItemTag<String> newString(String key) {
    return new SpItemTag<>(key, SpItemTag.Codec.STRING);
  }

  @Override
  public ItemTag<List<String>> newStringList(String key) {
    return new SpItemTag<>(key, SpItemTag.Codec.STRING_LIST);
  }

  @Override
  public ItemTag<Boolean> newBoolean(String key) {
    return new SpItemTag<>(key, SpItemTag.Codec.BOOLEAN);
  }
}
