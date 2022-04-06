package tc.oc.pgm.api.kits;

import com.google.common.collect.Range;
import javax.annotation.Nullable;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface Slot {

  String getKey();

  int getIndex();

  Range<Integer> getIndexRange();

  boolean isAuto();

  // If we support more inventory types, we would probably widen the type accepted by this method
  Inventory getInventory(HumanEntity holder);

  @Nullable
  ItemStack getItem(HumanEntity holder);

  @Nullable
  ItemStack putItem(HumanEntity holder, ItemStack stack);
}
