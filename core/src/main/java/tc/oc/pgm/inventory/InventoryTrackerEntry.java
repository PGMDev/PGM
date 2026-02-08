package tc.oc.pgm.inventory;

import lombok.Getter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;

@Getter
public class InventoryTrackerEntry {
  protected final Inventory watched;
  protected final Inventory preview;

  public InventoryTrackerEntry(Inventory watched, Inventory preview) {
    this.watched = watched;
    this.preview = preview;
  }

  public boolean isPlayerInventory() {
    return this.watched instanceof PlayerInventory;
  }

  public PlayerInventory getPlayerInventory() {
    return (PlayerInventory) this.watched;
  }
}
