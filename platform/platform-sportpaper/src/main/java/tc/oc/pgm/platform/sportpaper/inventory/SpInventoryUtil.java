package tc.oc.pgm.platform.sportpaper.inventory;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SpInventoryUtil implements InventoryUtils.InventoryUtilsPlatform {

  @Override
  public Collection<PotionEffect> getPotionEffects(ItemStack item) {
    return Potion.fromItemStack(item).getEffects();
  }

  @Override
  public boolean isUnbreakable(ItemMeta meta) {
    return meta.spigot().isUnbreakable();
  }

  @Override
  public void setUnbreakable(ItemMeta meta, boolean unbreakable) {
    meta.spigot().setUnbreakable(unbreakable);
  }

  @Override
  public boolean openVillager(Villager villager, Player viewer) {
    viewer.openMerchantCopy(villager);
    return true;
  }

  @Override
  public ItemStack craftItemCopy(ItemStack item) {
    return CraftItemStack.asCraftCopy(item);
  }

  @Override
  public void copyAttributeModifiers(ItemMeta destination, ItemMeta source) {
    for (String attribute : source.getModifiedAttributes()) {
      for (org.bukkit.attribute.AttributeModifier modifier :
          source.getAttributeModifiers(attribute)) {
        destination.addAttributeModifier(attribute, modifier);
      }
    }
  }

  @Override
  public void applyAttributeModifiers(
      SetMultimap<Attribute, AttributeModifier> modifiers, ItemMeta meta) {
    for (var entry : modifiers.entries()) {
      meta.addAttributeModifier(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public boolean attributesEqual(ItemMeta meta1, ItemMeta meta2) {
    var attributes = meta1.getModifiedAttributes();
    if (!attributes.equals(meta2.getModifiedAttributes())) return false;

    for (String attr : attributes) {
      if (modifiersDiffer(meta1.getAttributeModifiers(attr), meta2.getAttributeModifiers(attr)))
        return false;
    }
    return true;
  }

  @Override
  public void stripAttributes(ItemMeta meta) {
    for (String attr : meta.getModifiedAttributes()) {
      meta.getAttributeModifiers(attr).clear();
    }
  }

  @Override
  public EquipmentSlot getUsedHand(PlayerEvent event) {
    return EquipmentSlot.HAND;
  }

  @Override
  public void setCanDestroy(ItemMeta itemMeta, Set<Material> materials) {
    itemMeta.setCanDestroy(materials);
  }

  @Override
  public Set<Material> getCanDestroy(ItemMeta itemMeta) {
    return itemMeta.getCanDestroy();
  }

  @Override
  public void setCanPlaceOn(ItemMeta itemMeta, Set<Material> materials) {
    itemMeta.setCanPlaceOn(materials);
  }

  @Override
  public Set<Material> getCanPlaceOn(ItemMeta itemMeta) {
    return itemMeta.getCanPlaceOn();
  }
}
