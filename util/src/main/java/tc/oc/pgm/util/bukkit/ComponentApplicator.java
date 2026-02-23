package tc.oc.pgm.util.bukkit;

import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface ComponentApplicator {
  void apply(ItemStack stack);
}
