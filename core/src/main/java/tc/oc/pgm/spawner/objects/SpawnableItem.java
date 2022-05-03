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

  private final ItemStack stack;
  private final String metadataValue;

  public SpawnableItem(ItemStack stack, int spawnerId) {
    this.stack = stack;
    this.metadataValue = Integer.toString(spawnerId);
  }

  @Override
  public void spawn(Location location, Match match) {
    Item item = location.getWorld().dropItem(location, stack);
    item.setMetadata(Spawner.METADATA_KEY, new FixedMetadataValue(PGM.get(), metadataValue));
  }

  @Override
  public int getSpawnCount() {
    return stack.getAmount();
  }
}
