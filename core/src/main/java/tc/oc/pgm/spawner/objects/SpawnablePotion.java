package tc.oc.pgm.spawner.objects;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.spawner.Spawnable;
import tc.oc.pgm.spawner.Spawner;

public class SpawnablePotion implements Spawnable {
  private final ItemStack potionItem;
  private final String metadataValue;

  public SpawnablePotion(List<PotionEffect> potion, int spawnerId) {
    this.metadataValue = Integer.toString(spawnerId);
    ItemStack potionItem = new ItemStack(Material.POTION);
    PotionMeta potionMeta = (PotionMeta) potionItem.getItemMeta();
    for (PotionEffect effect : potion) {
      potionMeta.addCustomEffect(
          new PotionEffect((effect.getType()), effect.getDuration(), effect.getAmplifier()), false);
    }
    potionItem.setItemMeta(potionMeta);
    this.potionItem = potionItem;
  }

  @Override
  public void spawn(Location location, Match match) {
    ThrownPotion thrownPotion = location.getWorld().spawn(location, ThrownPotion.class);
    thrownPotion.setItem(potionItem);
    thrownPotion.setMetadata(
        Spawner.METADATA_KEY, new FixedMetadataValue(PGM.get(), metadataValue));
  }

  @Override
  public int getSpawnCount() {
    return potionItem.getAmount();
  }
}
