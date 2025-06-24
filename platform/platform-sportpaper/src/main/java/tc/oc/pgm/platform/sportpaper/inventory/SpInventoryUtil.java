package tc.oc.pgm.platform.sportpaper.inventory;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.util.Collection;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
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
  public EquipmentSlot getUsedHand(Event event) {
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
