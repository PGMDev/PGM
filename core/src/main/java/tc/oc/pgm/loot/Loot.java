package tc.oc.pgm.loot;

import org.bukkit.inventory.ItemStack;

public class Loot {
  private final ItemStack stack;
  private final String lootableID;

  public Loot(ItemStack stack, String lootableID) {
    this.stack = stack;
    this.lootableID = lootableID;
  }

  public ItemStack getStack() {
    return stack;
  }

  public String getLootableID() {
    return lootableID;
  }
}
