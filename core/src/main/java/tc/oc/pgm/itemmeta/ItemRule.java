package tc.oc.pgm.itemmeta;

import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.bukkit.ComponentApplicator;
import tc.oc.pgm.util.material.MaterialMatcher;

public record ItemRule(MaterialMatcher items, ComponentApplicator applicator) {
  public boolean matches(ItemStack stack) {
    return items.matches(stack);
  }

  public void apply(ItemStack stack) {
    applicator.apply(stack);
  }
}
