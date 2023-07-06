package tc.oc.pgm.util.material.matcher;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.MaterialDataProvider;

/**
 * A pattern that matches a specific Material and optionally, its metadata/damage value. If
 * constructed without the data value, the pattern will match only on the Material and ignore the
 * metadata/damage. If constructed with a data value, the pattern will only match world with that
 * metadata/damage value. In the latter case, Materials passed to the match() method will be assumed
 * to have a data value of 0, and will only match if 0 was also passed to the constructor.
 *
 * <p>The rationale is that only world that don't use their data value for identity will be passed
 * to the matches() method as Materials, and if the pattern is looking for a non-zero data on such a
 * world, it must be looking for a particular non-default state and thus should not match the
 * default state.
 */
public class SingleMaterialMatcher implements MaterialMatcher {
  private final MaterialData materialData;

  public SingleMaterialMatcher(MaterialData materialData) {
    this.materialData = materialData;
  }

  public SingleMaterialMatcher(Material material) {
    this.materialData = MaterialDataProvider.from(material);
  }

  public Material getMaterial() {
    return this.materialData.getMaterial();
  }

  @Override
  public Collection<Material> getMaterials() {
    return Collections.singleton(getMaterial());
  }

  public MaterialData getMaterialData() {
    return this.materialData;
  }

  @Override
  public boolean matches(Material material) {
    return this.materialData.matches(material);
  }

  @Override
  public boolean matches(MaterialData materialData) {
    return this.materialData.matches(materialData);
  }

  @Override
  public boolean matches(Block block) {
    return this.materialData.matches(block);
  }

  @Override
  public boolean matches(BlockState blockState) {
    return this.materialData.matches(blockState);
  }

  @Override
  public boolean matches(ItemStack stack) {
    return this.materialData.matches(stack);
  }

  public static SingleMaterialMatcher parse(String text) {
    MaterialData material = MaterialDataProvider.from(text);
    if (material == null) {
      throw new IllegalArgumentException("Could not find material '" + text + "'.");
    }
    return new SingleMaterialMatcher(material);
  }
}
