package tc.oc.pgm.platform.sportpaper.material;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;

@SuppressWarnings("deprecation")
public class ExactMaterialMatcher implements MaterialMatcher.Singular {
  private final Material material;
  private final byte data;

  public ExactMaterialMatcher(Material material, byte data) {
    this.material = material;
    this.data = data;
  }

  @Override
  public Set<Material> getMaterials() {
    return Set.of(material);
  }

  @Override
  public Material getMaterial() {
    return material;
  }

  @Override
  public boolean matches(Material material) {
    return material == this.material && this.data == 0;
  }

  @Override
  public boolean matches(MaterialData materialData) {
    return materialData.getItemType() == this.material
        && ((LegacyMaterialData) materialData).getData() == this.data;
  }

  @Override
  public boolean matches(ItemStack stack) {
    return stack.getType() == this.material && stack.getData().getData() == this.data;
  }

  @Override
  public Set<BlockMaterialData> getPossibleBlocks() {
    return Set.of(new SpMaterialData(this.material, this.data));
  }

  @Override
  public String toString() {
    return "ExactMaterialMatcher{" + "material=" + material + ", data=" + data + '}';
  }
}
