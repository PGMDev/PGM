package tc.oc.pgm.platform.modern.material;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.ItemMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.matcher.SingularMaterialMatcher;

public class ModernItemData implements ItemMaterialData {
  private final Material material;
  private final short damage;

  public ModernItemData(Material material) {
    this(material, (short) 0);
  }

  public ModernItemData(Material material, short damage) {
    this.material = material;
    this.damage = damage == 0 || material.getMaxDurability() == 0 ? 0 : damage;
  }

  @Override
  public ItemStack toItemStack(int amount) {
    //noinspection deprecation
    return new ItemStack(material, amount, damage);
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
    if (!(o instanceof MaterialData that)) return false;
    return material == that.getItemType();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(material);
  }
}
