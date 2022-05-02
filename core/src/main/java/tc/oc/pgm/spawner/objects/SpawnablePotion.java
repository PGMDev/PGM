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
  private ItemStack potionItem;
  private String METADATA_VALUE;

  public SpawnablePotion(List<PotionEffect> potion, int spawnerID) {
    this.METADATA_VALUE = Integer.toString(spawnerID);
    ItemStack potionItem = new ItemStack(Material.POTION);
    PotionMeta potionMeta = (PotionMeta) potionItem.getItemMeta();
    for (PotionEffect effect : potion) {
      int level = effect.getAmplifier();
      int duration = effect.getDuration();
      // type, level, overwrite
      potionMeta.addCustomEffect(new PotionEffect((effect.getType()), 20 * duration, level), false);
    }
    potionItem.setItemMeta(potionMeta);
    this.potionItem = potionItem;
  }

  @Override
  public void spawn(Location location, Match match) {
    ThrownPotion thrownPotion = location.getWorld().spawn(location, ThrownPotion.class);
    thrownPotion.setItem(potionItem);
    thrownPotion.getEffects();
    thrownPotion.setMetadata(
        Spawner.METADATA_KEY, new FixedMetadataValue(PGM.get(), METADATA_VALUE));
  }

  @Override
  public int getSpawnCount() {
    return 0;
  }
}
