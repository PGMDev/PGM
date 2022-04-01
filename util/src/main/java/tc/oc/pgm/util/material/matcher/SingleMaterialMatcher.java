package tc.oc.pgm.util.material.matcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.Materials;

/** A pattern that matches a specific Material. */
public class SingleMaterialMatcher implements MaterialMatcher {
  private final Material material;

  public SingleMaterialMatcher(Material material) {
    this.material = material;
  }

  public Material getMaterial() {
    return this.material;
  }

  @Override
  public Set<Material> getMaterials() {
    return Collections.singleton(getMaterial());
  }

  @Override
  public boolean matches(Material material) {
    return material == this.material;
  }

  @Override
  public boolean matches(BlockData blockData) {
    return blockData.getMaterial() == this.material;
  }

  @Override
  public boolean matches(ItemStack stack) {
    return stack.getType() == this.material;
  }

  public static SingleMaterialMatcher parse(String text) {
    Material material = Materials.parseMaterial(text);

    if (material == null) {
      throw new IllegalArgumentException("Could not find material '" + text + "'.");
    }
    return new SingleMaterialMatcher(material);
  }

  public static void main(String args[]) {
    List<Material> materials =
        Arrays.stream(Material.values()).filter(Material::isLegacy).collect(Collectors.toList());
    for (Material material : materials) {
      System.out.println(
          "mapguy.put(\""
              + material.name().substring(material.name().indexOf("_") + 1)
              + "\", Material."
              + material.name()
              + ");");
    }
  }
}
