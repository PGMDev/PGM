package tc.oc.pgm.util.inventory;

import com.google.common.collect.Range;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.Materials;

public class ItemMatcher {

  private final MaterialMatcher matcher;
  private final ItemStack base;
  private final Range<Integer> amount;

  private final boolean ignoreDurability;
  private final boolean ignoreMetadata;
  private final boolean ignoreName;
  private final boolean ignoreEnchantments;

  public ItemMatcher(
      MaterialMatcher matcher,
      ItemStack base,
      Range<Integer> amount,
      boolean ignoreDurability,
      boolean ignoreMetadata,
      boolean ignoreName,
      boolean ignoreEnchantments) {
    this.matcher = matcher;

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

      // Restore other meta if needed
      if (!ignoreName || !ignoreEnchantments) {
        ItemMeta original = item.getItemMeta();

        ItemMeta meta = newItem.getItemMeta();
        if (!ignoreName) meta.setDisplayName(original.getDisplayName());
        if (!ignoreEnchantments)
          original.getEnchants().forEach((e, l) -> meta.addEnchant(e, l, true));
        newItem.setItemMeta(meta);
      }

    } else {
      // Strip only specific parts
      ItemMeta meta = newItem.getItemMeta();
      if (ignoreName) meta.setDisplayName(null);
      if (ignoreEnchantments) meta.getEnchants().forEach((e, l) -> meta.removeEnchant(e));
      newItem.setItemMeta(meta);
    }

    return newItem;
  }

  public boolean hasAmount() {
    return !amount.equals(Range.atLeast(1));
  }

  public boolean matches(ItemStack query) {
    return query != null
        && amount.contains(query.getAmount())
        && (matcher != null ? matcher.matches(query) : Materials.itemsSimilarMaterial(base, query))
        && Materials.itemsSimilarDurability(base, query, ignoreDurability)
        && Materials.itemsSimilarMeta(base, stripMeta(query));
  }
}
