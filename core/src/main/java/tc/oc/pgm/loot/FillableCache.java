package tc.oc.pgm.loot;

import org.bukkit.inventory.Inventory;

public class FillableCache {
  private final Inventory inventory;
  private final String id;
  private final Cache cache;

  public FillableCache(Inventory inventory, String id, Cache cache) {
    this.inventory = inventory;
    this.id = id;
    this.cache = cache;
  }

  public Inventory getInventory() {
    return inventory;
  }

  public String getId() {
    return id;
  }

  public Cache getCache() {
    return cache;
  }
}
