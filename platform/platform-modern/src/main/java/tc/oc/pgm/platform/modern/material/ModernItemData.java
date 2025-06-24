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

  public ModernItemData(Material material) {
    this.material = material;
  }

  @Override
  public ItemStack toItemStack(int amount) {
    return new ItemStack(material, amount);
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
