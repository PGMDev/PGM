package tc.oc.pgm.spawner.objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.spawner.Spawnable;

public class SpawnablePotion implements Spawnable {

  private final int count;
  private final PotionEffect potion;

  public SpawnablePotion(int count, PotionEffect potion) {
    this.count = count;
    this.potion = potion;
  }

  @Override
  public void spawn(Location location) {
    for (int i = 0; i < count; i++) {
      ItemStack itemStack = new ItemStack(Material.POTION);
      PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
      potionMeta.addCustomEffect(potion, true);
      itemStack.setItemMeta(potionMeta);

      ThrownPotion thrownPotion =
          (ThrownPotion) location.getWorld().spawnEntity(location, EntityType.SPLASH_POTION);
      thrownPotion.setItem(itemStack);
    }
  }

  @Override
  public boolean isTracked() {
    return false;
  }

  @Override
  public int getSpawnCount() {
    return count;
  }
}
