package tc.oc.pgm.api.filter.query;

import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

/** Extended by queries where there <b>could</b> be an inventory */
public interface InventoryQuery extends Query {

  /**
   * Get the inventory if there is one
   *
   * @return the inventory or null if this query does not have one
   */
  @Nullable
  Inventory getInventory();
}
