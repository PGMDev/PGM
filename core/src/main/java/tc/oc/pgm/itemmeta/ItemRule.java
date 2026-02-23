package tc.oc.pgm.itemmeta;

import static tc.oc.pgm.util.attribute.AttributeUtils.ATTRIBUTE_UTILS;
import static tc.oc.pgm.util.inventory.InventoryUtils.INVENTORY_UTILS;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.bukkit.ComponentApplicator;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.material.MaterialMatcher;

public class ItemRule {
  final MaterialMatcher items;
  final PotionMeta meta;
  final @Nullable ComponentApplicator applicator;

  public ItemRule(
      MaterialMatcher items, PotionMeta meta, @Nullable ComponentApplicator applicator) {
    this.items = items;
    this.meta = meta;
    this.applicator = applicator;
  }

  public boolean matches(ItemStack stack) {
    return items.matches(stack);
  }

  public void apply(ItemStack stack) {
    InventoryUtils.addEffects(stack, this.meta.getCustomEffects());

    ItemMeta meta = stack.getItemMeta();
    if (meta != null) {
      if (this.meta.hasDisplayName()) {
        meta.setDisplayName(this.meta.getDisplayName());
      }

      if (this.meta.hasLore()) {
        meta.setLore(this.meta.getLore());
      }

      Set<ItemFlag> flags = this.meta.getItemFlags();
      meta.addItemFlags(flags.toArray(new ItemFlag[0]));

      InventoryUtils.addEnchantments(meta, this.meta.getEnchants());

      ATTRIBUTE_UTILS.copyAttributeModifiers(meta, this.meta);

      Set<Material> canDestroy = unionMaterials(
          INVENTORY_UTILS.getCanDestroy(meta), INVENTORY_UTILS.getCanDestroy(this.meta));
      Set<Material> canPlaceOn = unionMaterials(
          INVENTORY_UTILS.getCanPlaceOn(meta), INVENTORY_UTILS.getCanPlaceOn(this.meta));

      if (!canDestroy.isEmpty()) INVENTORY_UTILS.setCanDestroy(meta, canDestroy);
      if (!canPlaceOn.isEmpty()) INVENTORY_UTILS.setCanPlaceOn(meta, canPlaceOn);

      if (INVENTORY_UTILS.isUnbreakable(this.meta)) INVENTORY_UTILS.setUnbreakable(meta, true);

      stack.setItemMeta(meta);
    }

    if (applicator != null) applicator.apply(stack);
  }

  private Set<Material> unionMaterials(Set<Material> a, Set<Material> b) {
    if (a.containsAll(b)) return a;
    if (b.containsAll(a)) return b;

    Set<Material> union = EnumSet.copyOf(a);
    union.addAll(b);
    return union;
  }
}
