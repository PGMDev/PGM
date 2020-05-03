package tc.oc.pgm.spawner.objects;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.spawner.Spawnable;
import tc.oc.pgm.spawner.Spawner;

public class SpawnableItem implements Spawnable {

  private ItemStack stack;
  private static final String METADATA_VALUE = "spawner-item";

  public SpawnableItem(ItemStack stack) {
    this.stack = stack;
  }

  @Override
  public void spawn(Location location, Match match) {
    Item item = location.getWorld().dropItem(location, stack);
    item.setMetadata(Spawner.METADATA_KEY, new FixedMetadataValue(PGM.get(), METADATA_VALUE));
  }

  @Override
  public boolean isTracked() {
    return true;
  }

  @Override
  public int getSpawnCount() {
    return stack.getAmount();
  }
}
