package tc.oc.pgm.kits.tag;

import tc.oc.util.bukkit.item.tag.BooleanItemTag;
import tc.oc.util.bukkit.item.tag.StringItemTag;

public class ItemTags {

  public static final BooleanItemTag PREVENT_SHARING = new BooleanItemTag("prevent-sharing", false);
  public static final StringItemTag PROJECTILE = new StringItemTag("projectile", null);
  public static final StringItemTag ORIGINAL_NAME = new StringItemTag("original-name", null);

  private ItemTags() {}
}
