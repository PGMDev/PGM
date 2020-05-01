package tc.oc.pgm.kits.tag;

import tc.oc.pgm.util.inventory.tag.ItemTag;

public class ItemTags {

  public static final ItemTag<Boolean> PREVENT_SHARING = ItemTag.newBoolean("prevent-sharing");
  public static final ItemTag<String> PROJECTILE = ItemTag.newString("projectile");
  public static final ItemTag<String> ORIGINAL_NAME = ItemTag.newString("original-name");

  private ItemTags() {}
}
