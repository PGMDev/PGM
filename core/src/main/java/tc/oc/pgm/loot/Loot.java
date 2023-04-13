package tc.oc.pgm.loot;

import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.util.compose.Composition;

public class Loot extends SelfIdentifyingFeatureDefinition {

  private final Composition<ItemStack> loot;

  public Loot(String id, Composition<ItemStack> loot) {
    super(id);
    this.loot = loot;
  }

  public Composition<ItemStack> lootItems() {
    return this.loot;
  }
}
