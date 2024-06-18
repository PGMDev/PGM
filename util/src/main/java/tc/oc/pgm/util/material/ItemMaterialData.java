package tc.oc.pgm.util.material;

import org.bukkit.inventory.ItemStack;

public interface ItemMaterialData extends MaterialData {

  ItemStack toItemStack(int amount);
}
