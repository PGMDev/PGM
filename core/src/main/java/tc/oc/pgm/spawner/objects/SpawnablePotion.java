package tc.oc.pgm.spawner.objects;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.spawner.Spawnable;
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
      // overwrite = false
      potionMeta.addCustomEffect(effect, false);
    }
    potionItem.setItemMeta(potionMeta);
    this.potionItem = potionItem;
  }

  @Override
  public void spawn(Location location, Match match) {
    new NMSHacks.EntityPotion(location, potionItem).spawn();
    // TODO set metadata when necessary
  }

  @Override
  public int getSpawnCount() {
    return potionItem.getAmount();
  }
}
