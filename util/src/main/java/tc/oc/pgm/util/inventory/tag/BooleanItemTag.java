package tc.oc.pgm.util.inventory.tag;

import org.bukkit.inventory.ItemStack;

/** An item tag that stores boolean values. */
final class BooleanItemTag implements ItemTag<Boolean> {

  private final ItemTag<String> itemTag;

  BooleanItemTag(ItemTag<String> itemTag) {
    this.itemTag = itemTag;
  }

  @Override
  public Boolean get(ItemStack item) {
    return "1".equals(itemTag.get(item)) ? true : null;
  }

  @Override
  public void set(ItemStack item, Boolean value) {
    itemTag.set(item, value ? "1" : "0");
  }

  @Override
  public void clear(ItemStack item) {
    itemTag.clear(item);
  }
}
