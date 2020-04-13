package tc.oc.pgm.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;

public class InventoryTrackerEntry {
  protected final Inventory watched;
  protected final Inventory preview;

  public InventoryTrackerEntry(Inventory watched, Inventory preview) {
    this.watched = watched;
    this.preview = preview;
  }

  public Inventory getWatched() {
    return this.watched;
  }

  public boolean isPlayerInventory() {
    return this.watched instanceof PlayerInventory;
  }

  public PlayerInventory getPlayerInventory() {
    return (PlayerInventory) this.watched;
  }

  public Inventory getPreview() {
    return this.preview;
  }
}
