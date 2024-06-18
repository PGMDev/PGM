package tc.oc.pgm.platform.v1_20_6.material;

import static org.bukkit.potion.PotionType.*;
import static org.bukkit.potion.PotionType.STRONG_HEALING;
import static org.bukkit.potion.PotionType.STRONG_LEAPING;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import tc.oc.pgm.util.material.ItemMaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.matcher.SingularMaterialMatcher;

public class PotionMaterialData implements ItemMaterialData {

  private static final PotionStack[] TYPES = new PotionStack[] {
    /*  0 */ new PotionStack(WATER, AWKWARD, THICK),
    /*  1 */ new PotionStack(REGENERATION, LONG_REGENERATION, STRONG_REGENERATION),
    /*  2 */ new PotionStack(SWIFTNESS, LONG_SWIFTNESS, STRONG_SWIFTNESS),
    /*  3 */ new PotionStack(FIRE_RESISTANCE, LONG_FIRE_RESISTANCE, null),
    /*  4 */ new PotionStack(POISON, LONG_POISON, STRONG_POISON),
    /*  5 */ new PotionStack(HEALING, null, STRONG_HEALING),
    /*  6 */ new PotionStack(NIGHT_VISION, LONG_NIGHT_VISION, null),
    /*  7 */ new PotionStack(null, null, null),
    /*  8 */ new PotionStack(WEAKNESS, LONG_WEAKNESS, null),
    /*  9 */ new PotionStack(STRENGTH, LONG_STRENGTH, STRONG_STRENGTH),
    /* 10 */ new PotionStack(SLOWNESS, LONG_SLOWNESS, STRONG_SLOWNESS),
    /* 11 */ new PotionStack(LEAPING, LONG_LEAPING, STRONG_LEAPING),
    /* 12 */ new PotionStack(HARMING, null, STRONG_HARMING),
    /* 13 */ new PotionStack(WATER_BREATHING, LONG_WATER_BREATHING, null),
    /* 14 */ new PotionStack(INVISIBILITY, LONG_INVISIBILITY, null),
    /* 15 */ new PotionStack(null, null, null)
  };

  record PotionStack(PotionType base, PotionType extended, PotionType strong) {
    PotionType get(boolean extended, boolean strong) {
      if (strong && this.strong != null) return this.strong;
      if (extended && this.extended != null) return this.extended;
      return base;
    }
  }

  private final Material material;
  private final PotionType potionType;

  public PotionMaterialData(short damage) {
    boolean strong = (damage & (1 << 5)) != 0;
    boolean extended = (damage & (1 << 6)) != 0;
    boolean splash = (damage & (1 << 14)) != 0;

    this.material = splash ? Material.SPLASH_POTION : Material.POTION;
    this.potionType = TYPES[damage & 15].get(extended, strong);
  }

  @Override
  public ItemStack toItemStack(int amount) {
    ItemStack is = new ItemStack(material, amount);
    if (potionType != null) {
      PotionMeta meta = (PotionMeta) is.getItemMeta();
      meta.setBasePotionType(potionType);
      is.setItemMeta(meta);
    }
    return is;
  }

  @Override
  public Material getItemType() {
    return material;
  }

  @Override
  public MaterialMatcher toMatcher() {
    return SingularMaterialMatcher.of(material);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PotionMaterialData that)) return false;
    return material == that.material && potionType == that.potionType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(material, potionType);
  }
}
