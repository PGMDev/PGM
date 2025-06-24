package tc.oc.pgm.util.inventory;

import com.google.common.collect.Range;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.util.material.Materials;

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

    this.ignoreDurability = ignoreDurability;

    this.ignoreMetadata = ignoreMetadata;
    this.ignoreName = ignoreName;
    this.ignoreEnchantments = ignoreEnchantments;

    this.amount = amount;
    this.base = stripMeta(base);
  }

  private ItemStack stripMeta(final ItemStack item) {
    // No modification needed
    if (!item.hasItemMeta() || (!ignoreMetadata && !ignoreName && !ignoreEnchantments)) return item;

    var newItem = item.clone();
    if (ignoreMetadata) {
      // Strip all meta, then re-add if needed
      newItem.setItemMeta(null);

      // Restore name or enchants
      if (!ignoreName) newItem.getItemMeta().setDisplayName(item.getItemMeta().getDisplayName());
      if (!ignoreEnchantments) newItem.addUnsafeEnchantments(item.getEnchantments());
    } else {
      // Strip only specific parts
      ItemMeta meta = item.getItemMeta();

      if (ignoreName) meta.setDisplayName(null);
      if (ignoreEnchantments) item.getEnchantments().keySet().forEach(item::removeEnchantment);
    }

    return newItem;
  }

  public boolean matches(ItemStack query) {
    return Materials.itemsSimilar(base, stripMeta(query), ignoreDurability)
        && amount.contains(query.getAmount());
  }
}
