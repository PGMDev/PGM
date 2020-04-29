package tc.oc.pgm.spawner.objects;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawner.Spawnable;
import tc.oc.pgm.spawner.Spawner;
import tc.oc.pgm.util.nms.NMSHacks;

public class SpawnableItem implements Spawnable {

  private int count;
  private ItemStack stack;
  private String metadataValue = "Spawner Item";

  public SpawnableItem(ItemStack stack) {
    this.count = stack.getAmount();
    this.stack = stack;
  }

  @Override
  public void spawn(Location location, Match match) {
    Item item = location.getWorld().dropItem(location.add(0, 1, 0), stack);
    item.setMetadata(Spawner.METADATA_KEY, new FixedMetadataValue(PGM.get(), metadataValue));

    Object particle =
        NMSHacks.particlesPacket(
            "FLAME", true, location.toVector(), new Vector(0.15, 0.15, 0.15), 0, 40);
    for (MatchPlayer player : match.getPlayers()) {
      NMSHacks.sendPacket(player.getBukkit(), particle);
    }
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
