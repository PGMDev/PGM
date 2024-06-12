package tc.oc.pgm.platform.sportpaper.material;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.IBlockData;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SpSingleMaterialMatcher implements SingleMaterialMatcher.Factory {

  @Override
  public SingleMaterialMatcher of(Material material) {
    return new SingleMaterialMatcherImpl(material);
  }

  @Override
  public SingleMaterialMatcher of(MaterialData md) {
    return new SingleMaterialMatcherImpl(md);
  }

  @Override
  public SingleMaterialMatcher parse(String text) {
    String[] pieces = text.split(":");
    Material material = Materials.parseMaterial(pieces[0]);
    if (material == null) {
      throw new IllegalArgumentException("Could not find material '" + pieces[0] + "'.");
    }
    if (pieces.length > 1) {
      try {
        return new SingleMaterialMatcherImpl(material, Byte.parseByte(pieces[1]));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid damage value: " + pieces[1], e);
      }
    } else {
      return new SingleMaterialMatcherImpl(material);
    }
  }

  @SuppressWarnings("deprecation")
  static class SingleMaterialMatcherImpl implements SingleMaterialMatcher {
    private final Material material;

    private final byte data;
    private final boolean dataMatters;

    public SingleMaterialMatcherImpl(Material material, byte data) {
      this.material = material;
      this.data = data;
      this.dataMatters = true;
    }

    public SingleMaterialMatcherImpl(MaterialData md) {
      this.material = md.getItemType();
      this.data = md.getBukkit().getData();
      this.dataMatters = true;
    }

    public SingleMaterialMatcherImpl(Material material) {
      this.material = material;
      this.data = 0;
      this.dataMatters = false;
    }

    @Override
    public Set<Material> getMaterials() {
      return Collections.singleton(material);
    }

    @Override
    public boolean matches(Material material) {
      return material == this.material && (!this.dataMatters || this.data == 0);
    }

    @Override
    public boolean matches(org.bukkit.material.MaterialData materialData) {
      return materialData.getItemType() == this.material
          && (!this.dataMatters || materialData.getData() == this.data);
    }

    @Override
    public boolean matches(MaterialData materialData) {
      return materialData.getItemType() == this.material
          && (!this.dataMatters || materialData.getBukkit().getData() == this.data);
    }

    @Override
    public boolean matches(ItemStack stack) {
      return stack.getType() == this.material
          && (!this.dataMatters || stack.getData().getData() == this.data);
    }

    @Override
    public Set<MaterialData> getMaterialData() {
      if (dataMatters) {
        return Collections.singleton(MaterialData.from(this.material.getNewData(this.data)));
      }

      // Get all possible blockstates off of nms
      Block block = CraftMagicNumbers.getBlock(material);
      List<IBlockData> states = block.P().a();
      Set<MaterialData> materials = new HashSet<>(states.size());
      for (IBlockData state : states) {
        materials.add(MaterialData.from(material.getNewData((byte) block.toLegacyData(state))));
      }
      return materials;
    }

    @Override
    public void addIngredient(ShapelessRecipe recipe, int count) {
      if (dataMatters) {
        recipe.addIngredient(count, material, data);
      } else {
        recipe.addIngredient(count, material);
      }
    }

    @Override
    public void setIngredient(ShapedRecipe recipe, char key) {
      if (dataMatters) {
        recipe.setIngredient(key, material, data);
      } else {
        recipe.setIngredient(key, material);
      }
    }

    @Override
    public FurnaceRecipe createFurnaceRecipe(ItemStack result) {
      if (dataMatters) {
        return new FurnaceRecipe(result, material, data);
      } else {
        return new FurnaceRecipe(result, material);
      }
    }

    @Override
    public String toString() {
      return "SingleMaterialMatcher{"
          + "material="
          + material
          + (dataMatters ? ", data=" + data : "")
          + '}';
    }
  }
}
