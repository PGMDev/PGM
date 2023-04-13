package tc.oc.pgm.kits.tag;

import tc.oc.pgm.util.inventory.tag.ItemTag;

public class ItemTags {

  public static final ItemTag<Boolean> PREVENT_SHARING = ItemTag.newBoolean("prevent-sharing");
  public static final ItemTag<String> PROJECTILE = ItemTag.newString("projectile");
  public static final ItemTag<String> CONSUMABLE = ItemTag.newString("consumable");
  public static final ItemTag<String> ORIGINAL_NAME = ItemTag.newString("original-name");
  public static final ItemTag<Boolean> INFINITE = ItemTag.newBoolean("infinite");
  public static final ItemTag<Boolean> LOCKED = ItemTag.newBoolean("locked");

  private ItemTags() {}
}
