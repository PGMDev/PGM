package tc.oc.pgm.spawner.objects;

import static tc.oc.pgm.util.bukkit.BukkitUtils.parse;
import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.spawner.Spawnable;
import tc.oc.pgm.spawner.Spawner;

public class SpawnablePotion implements Spawnable {
  // Newer versions use SPLASH_POTION
  private static final Material SPLASH_POTION = parse(Material::valueOf, "SPLASH_POTION", "POTION");

  private static final int SPLASH_BIT = 0x4000;

  private final ItemStack potionItem;
  private final String spawnerId;

  public SpawnablePotion(List<PotionEffect> potion, PotionType main, String spawnerId) {
    this.spawnerId = spawnerId;
    ItemStack potionItem = new ItemStack(SPLASH_POTION, 1, (short) SPLASH_BIT);
    PotionMeta potionMeta = (PotionMeta) potionItem.getItemMeta();
    if (main != null) potionMeta.setMainEffect(main.getEffectType());
    for (PotionEffect effect : potion) {
      potionMeta.addCustomEffect(effect, false);
    }
    potionItem.setItemMeta(potionMeta);
    this.potionItem = potionItem;
  }

  @Override
  public void spawn(Location location, Match match) {
    ThrownPotion potion = MISC_UTILS.spawnPotion(location, potionItem);
    potion.setMetadata(Spawner.METADATA_KEY, new FixedMetadataValue(PGM.get(), spawnerId));
  }

  @Override
  public int getSpawnCount() {
    return 1;
  }
}
