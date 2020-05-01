package tc.oc.pgm.util.material.matcher;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.Materials;

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
  private final Material material;

  private final byte data;
  private final boolean dataMatters;

  public SingleMaterialMatcher(Material material, byte data) {
    this.material = material;
    this.data = data;
    this.dataMatters = true;
  }

  public SingleMaterialMatcher(MaterialData materialData) {
    this.material = materialData.getItemType();
    this.data = materialData.getData();
    this.dataMatters = true;
  }

  public SingleMaterialMatcher(Material material) {
    this.material = material;
    this.data = 0;
    this.dataMatters = false;
  }

  public Material getMaterial() {
    return this.material;
  }

  @Override
  public Collection<Material> getMaterials() {
    return Collections.singleton(getMaterial());
  }

  public byte getData() {
    return this.data;
  }

  @SuppressWarnings("deprecation")
  public MaterialData getMaterialData() {
    return this.material.getNewData(this.data);
  }

  public boolean dataMatters() {
    return this.dataMatters;
  }

  @Override
  public boolean matches(Material material) {
    return material == this.material && (!this.dataMatters || this.data == 0);
  }

  @Override
  public boolean matches(MaterialData materialData) {
    return materialData.getItemType() == this.material
        && (!this.dataMatters || materialData.getData() == this.data);
  }

  @Override
  public boolean matches(ItemStack stack) {
    return stack.getType() == this.material
        && (!this.dataMatters || stack.getData().getData() == this.data);
  }

  public static SingleMaterialMatcher parse(String text) {
    String[] pieces = text.split(":");
    Material material = Materials.parseMaterial(pieces[0]);
    if (material == null) {
      throw new IllegalArgumentException("Could not find material '" + pieces[0] + "'.");
    }
    if (pieces.length > 1) {
      try {
        return new SingleMaterialMatcher(material, Byte.parseByte(pieces[1]));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid damage value: " + pieces[1], e);
      }
    } else {
      return new SingleMaterialMatcher(material);
    }
  }
}
