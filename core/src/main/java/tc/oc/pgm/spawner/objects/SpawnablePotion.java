package tc.oc.pgm.spawner.objects;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.spawner.Spawnable;
import tc.oc.pgm.spawner.Spawner;
import tc.oc.pgm.util.nms.NMSHacks;

public class SpawnablePotion implements Spawnable {
  private final ItemStack potionItem;
  private final String spawnerId;

  public SpawnablePotion(List<PotionEffect> potion, int damageValue, String spawnerId) {
    this.spawnerId = spawnerId;
    // Potion "name" determines potion color
    ItemStack potionItem = new Potion(damageValue).splash().toItemStack(1);
    PotionMeta potionMeta = (PotionMeta) potionItem.getItemMeta();
    for (PotionEffect effect : potion) {
      potionMeta.addCustomEffect(effect, false);
    }
    potionItem.setItemMeta(potionMeta);
    this.potionItem = potionItem;
  }

  @Override
  public void spawn(Location location, Match match) {
    NMSHacks.EntityPotion entityPotion = new NMSHacks.EntityPotion(location, potionItem);
    entityPotion.spawn();
    entityPotion
        .getBukkitEntity()
        .setMetadata(Spawner.METADATA_KEY, new FixedMetadataValue(PGM.get(), spawnerId));
  }

  @Override
  public int getSpawnCount() {
    return 1;
  }
}
