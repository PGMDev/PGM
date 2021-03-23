package tc.oc.pgm.itemmeta;

import java.util.Collection;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.nms.NMSHacks;

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

      if (BukkitUtils.isSportPaper()) {
        for (String attribute : this.meta.getModifiedAttributes()) {
          for (AttributeModifier modifier : this.meta.getAttributeModifiers(attribute)) {
            meta.addAttributeModifier(attribute, modifier);
          }
        }
      } else {
        // TODO: Do this with reflection
      }
      Collection<Material> canDestroy = NMSHacks.getCanDestroy(meta);
      canDestroy.addAll(NMSHacks.getCanDestroy(this.meta));
      NMSHacks.setCanDestroy(meta, canDestroy);

      Collection<Material> canPlaceOn = NMSHacks.getCanPlaceOn(meta);
      canPlaceOn.addAll(NMSHacks.getCanPlaceOn(this.meta));
      NMSHacks.setCanPlaceOn(meta, canPlaceOn);

      if (this.meta.spigot().isUnbreakable()) meta.spigot().setUnbreakable(true);

      stack.setItemMeta(meta);
    }
  }
}
