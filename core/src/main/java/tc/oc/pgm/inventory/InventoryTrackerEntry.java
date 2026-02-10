package tc.oc.pgm.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;

public record InventoryTrackerEntry(Inventory watched, Inventory preview) {

  public boolean isPlayerInventory() {
    return this.watched instanceof PlayerInventory;
  }

  public PlayerInventory getPlayerInventory() {
    return (PlayerInventory) this.watched;
  }

  @Deprecated
  public Inventory getWatched() {
    return watched();
  }

  @Deprecated
  public Inventory getPreview() {
    return preview();
  }
}
