package tc.oc.pgm.util.inventory;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public interface OldPotionBuilder {

  static Map<Integer, PotionType> POTION_TYPE_MAP = buildPotionTypeMap();

  static Map<Integer, PotionType> buildPotionTypeMap() {
    HashMap<Integer, PotionType> typeMap = new HashMap<>();

    typeMap.put(1, PotionType.REGEN);
    typeMap.put(2, PotionType.SPEED);
    typeMap.put(3, PotionType.FIRE_RESISTANCE);
    typeMap.put(4, PotionType.POISON);
    typeMap.put(5, PotionType.INSTANT_HEAL);
    typeMap.put(6, PotionType.NIGHT_VISION);

    typeMap.put(8, PotionType.WEAKNESS);
    typeMap.put(9, PotionType.STRENGTH);
    typeMap.put(10, PotionType.SLOWNESS);
    typeMap.put(11, PotionType.JUMP);
    typeMap.put(12, PotionType.INSTANT_DAMAGE);
    typeMap.put(13, PotionType.WATER_BREATHING);
    typeMap.put(14, PotionType.INVISIBILITY);

    // These are non-standard
    typeMap.put(0, PotionType.WATER);
    typeMap.put(7, PotionType.MUNDANE);
    typeMap.put(15, PotionType.AWKWARD);

    return typeMap;
  }

  static ItemStack buildLegacyPotion(short damage, int amount) {
    ItemStack itemStack =
        new ItemStack(
            (damage & (1 << 14)) != 0 ? Material.SPLASH_POTION : Material.POTION, amount, damage);

    PotionMeta meta = (PotionMeta) itemStack.getItemMeta();

    // First 4 bytes are potion type
    PotionType potionType = POTION_TYPE_MAP.get(damage & 15);

    boolean extendedPotion = (damage & (1 << 6)) != 0 && potionType.isExtendable();
    boolean levelTwoPotion =
        (damage & (1 << 5)) != 0 && potionType.isUpgradeable() && !extendedPotion;

    PotionData potionData = new PotionData(potionType, extendedPotion, levelTwoPotion);

    meta.setBasePotionData(potionData);
    itemStack.setItemMeta(meta);

    return itemStack;
  }
}
