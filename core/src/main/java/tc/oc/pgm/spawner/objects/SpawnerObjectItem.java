package tc.oc.pgm.spawner.objects;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.spawner.SpawnerModule;
import tc.oc.pgm.spawner.SpawnerObject;

public class SpawnerObjectItem implements SpawnerObject {

  private int count;
  private ItemStack stack;

  public SpawnerObjectItem(ItemStack stack) {
    this.count = stack.getAmount();
    this.stack = stack;
  }

  @Override
  public void spawn(Location location) {
    Item item = location.getWorld().dropItem(location, stack);
    item.setMetadata(SpawnerModule.METADATA_KEY, new FixedMetadataValue(PGM.get(), "Spawner Item"));
  }

  @Override
  public boolean isTracked() {
    return true;
  }

  @Override
  public int spawnCount() {
    return count;
  }
}
