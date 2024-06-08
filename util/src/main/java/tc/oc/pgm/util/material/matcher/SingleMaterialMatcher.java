package tc.oc.pgm.util.material.matcher;

import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.platform.Platform;

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
public interface SingleMaterialMatcher extends MaterialMatcher {

  SingleMaterialMatcher.Factory FACTORY =
      Platform.requireInstance(SingleMaterialMatcher.Factory.class);

  static SingleMaterialMatcher of(Material material) {
    return FACTORY.of(material);
  }

  static SingleMaterialMatcher of(org.bukkit.material.MaterialData material) {
    return of(MaterialData.from(material));
  }

  static SingleMaterialMatcher of(MaterialData materialdata) {
    return FACTORY.of(materialdata);
  }

  static SingleMaterialMatcher parse(String pattern) {
    return FACTORY.parse(pattern);
  }

  Collection<MaterialData> getBlockStates();

  void addIngredient(ShapelessRecipe recipe, int count);

  void setIngredient(ShapedRecipe recipe, char key);

  FurnaceRecipe createFurnaceRecipe(ItemStack result);

  interface Factory {
    SingleMaterialMatcher of(Material material);

    SingleMaterialMatcher of(MaterialData material);

    SingleMaterialMatcher parse(String pattern);
  }
}
