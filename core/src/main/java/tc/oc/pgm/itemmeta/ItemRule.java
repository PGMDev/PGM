package tc.oc.pgm.itemmeta;

import com.google.common.collect.Sets;
import java.util.Set;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.util.ImmutableMaterialSet;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.material.MaterialMatcher;

public class ItemRule {
  final MaterialMatcher items;
  final PotionMeta meta;

  public ItemRule(MaterialMatcher items, PotionMeta meta) {
    this.items = items;
    this.meta = meta;
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
      meta.addItemFlags(flags.toArray(new ItemFlag[flags.size()]));

      InventoryUtils.addEnchantments(meta, this.meta.getEnchants());

      for (String attribute : this.meta.getModifiedAttributes()) {
        for (AttributeModifier modifier : this.meta.getAttributeModifiers(attribute)) {
          meta.addAttributeModifier(attribute, modifier);
        }
      }

      if (this.meta.spigot().isUnbreakable()) meta.spigot().setUnbreakable(true);
      meta.setCanDestroy(unionMaterials(meta.getCanDestroy(), this.meta.getCanDestroy()));
      meta.setCanPlaceOn(unionMaterials(meta.getCanPlaceOn(), this.meta.getCanPlaceOn()));

      stack.setItemMeta(meta);
    }
  }

  private static ImmutableMaterialSet unionMaterials(
      ImmutableMaterialSet a, ImmutableMaterialSet b) {
    if (a.containsAll(b)) return a;
    if (b.containsAll(a)) return b;
    return ImmutableMaterialSet.of(Sets.union(a, b));
  }
}
