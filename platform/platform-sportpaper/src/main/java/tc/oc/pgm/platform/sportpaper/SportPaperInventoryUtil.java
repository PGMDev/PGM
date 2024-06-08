package tc.oc.pgm.platform.sportpaper;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.util.Collection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SportPaperInventoryUtil implements InventoryUtils.InventoryUtilsPlatform {

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
}
