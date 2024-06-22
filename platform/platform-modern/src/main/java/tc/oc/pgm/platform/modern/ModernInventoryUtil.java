package tc.oc.pgm.platform.modern;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.platform.modern.attribute.AttributeUtilBukkit;
import tc.oc.pgm.util.attribute.AttributeModifier;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernInventoryUtil implements InventoryUtils.InventoryUtilsPlatform {

  @Override
  public Collection<PotionEffect> getPotionEffects(ItemStack item) {
    if (item.getItemMeta() instanceof PotionMeta potion) {
      var base = potion.getBasePotionType();
      if (base != null) return base.getPotionEffects();
    }
    return Collections.emptyList();
  }

  @Override
  public boolean isUnbreakable(ItemMeta meta) {
    return meta.isUnbreakable();
  }

  @Override
  public void setUnbreakable(ItemMeta meta, boolean unbreakable) {
    meta.setUnbreakable(unbreakable);
  }

  @Override
  public boolean openVillager(Villager villager, Player viewer) {
    viewer.openMerchant((Villager) villager.copy(), true);
    return true;
  }

  @Override
  public ItemStack craftItemCopy(ItemStack item) {
    return CraftItemStack.asCraftCopy(item);
  }

  @Override
  public void copyAttributeModifiers(ItemMeta destination, ItemMeta source) {
    var modifiers = source.getAttributeModifiers();
    if (modifiers != null) modifiers.forEach(destination::addAttributeModifier);
  }

  @Override
  public void applyAttributeModifiers(
      SetMultimap<tc.oc.pgm.util.attribute.Attribute, AttributeModifier> attributeModifiers,
      ItemMeta meta) {
    for (var entry : attributeModifiers.entries()) {
      meta.addAttributeModifier(
          AttributeUtilBukkit.toBukkit(entry.getKey()),
          AttributeUtilBukkit.toBukkit(entry.getValue()));
    }
  }

  @Override
  public void setCanDestroy(ItemMeta itemMeta, Collection<Material> materials) {
    // TODO: PLATFORM 1.20 no support for can place/destroy
  }

  @Override
  public Set<Material> getCanDestroy(ItemMeta itemMeta) {
    // TODO: PLATFORM 1.20 no support for can place/destroy
    return Collections.emptySet();
  }

  @Override
  public void setCanPlaceOn(ItemMeta itemMeta, Collection<Material> materials) {
    // TODO: PLATFORM 1.20 no support for can place/destroy
  }

  @Override
  public Set<Material> getCanPlaceOn(ItemMeta itemMeta) {
    // TODO: PLATFORM 1.20 no support for can place/destroy
    return Collections.emptySet();
  }
}
