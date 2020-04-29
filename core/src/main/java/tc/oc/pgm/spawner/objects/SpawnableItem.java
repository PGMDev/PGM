package tc.oc.pgm.spawner.objects;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.spawner.Spawner;
import tc.oc.pgm.spawner.Spawnable;

public class SpawnableItem implements Spawnable {

  private int count;
  private ItemStack stack;

  public SpawnableItem(ItemStack stack) {
    this.count = stack.getAmount();
    this.stack = stack;
  }

  @Override
  public void spawn(Location location) {
    Item item = location.getWorld().dropItem(location, stack);
    item.setMetadata(Spawner.METADATA_KEY, new FixedMetadataValue(PGM.get(), "Spawner Item"));
  }

  @Override
  public boolean isTracked() {
    return true;
  }

  @Override
  public int getSpawnCount() {
    return count;
  }
}
