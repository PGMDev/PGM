package tc.oc.pgm.util.inventory;

import com.google.common.collect.Range;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemMatcher {

  private final ItemStack base;
  private final Range<Integer> amount;

  private final boolean ignoreDurability;
  private final boolean ignoreMetadata;
  private final boolean ignoreName;
  private final boolean ignoreEnchantments;

  public ItemMatcher(
      ItemStack base,
      Range<Integer> amount,
      boolean ignoreDurability,
      boolean ignoreMetadata,
      boolean ignoreName,
      boolean ignoreEnchantments) {
    if (ignoreMetadata && (!ignoreName || !ignoreEnchantments))
      throw new UnsupportedOperationException(
          "Cannot ignore metadata but respect name or enchantments");

    this.ignoreDurability = ignoreDurability;

    this.ignoreMetadata = ignoreMetadata;
    this.ignoreName = ignoreName;
    this.ignoreEnchantments = ignoreEnchantments;

    this.amount = amount;
    this.base = stripMeta(base);
  }

  private ItemStack stripMeta(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null || (!ignoreMetadata && !(ignoreEnchantments && meta.hasEnchants())))
      return item;

    item = item.clone();
    if (ignoreMetadata) item.setItemMeta(null);
    else item.getEnchantments().keySet().forEach(item::removeEnchantment);

    return item;
  }

  public boolean matches(ItemStack query) {
    return base.isSimilar(stripMeta(query), ignoreDurability, ignoreName)
        && amount.contains(query.getAmount());
  }
}
