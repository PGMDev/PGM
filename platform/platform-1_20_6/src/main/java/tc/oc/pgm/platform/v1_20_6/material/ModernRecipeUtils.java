package tc.oc.pgm.platform.v1_20_6.material;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.RecipeUtils;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernRecipeUtils implements RecipeUtils {

  private NamespacedKey getKey() {
    return NamespacedKey.fromString(UUID.randomUUID().toString(), BukkitUtils.getPlugin());
  }

  @Override
  public ShapedRecipe createShapedRecipe(ItemStack result) {
    return new ShapedRecipe(getKey(), result);
  }

  @Override
  public ShapelessRecipe createShapelessRecipe(ItemStack result) {
    return new ShapelessRecipe(getKey(), result);
  }

  @Override
  public void addIngredient(Node node, ShapelessRecipe recipe, int count)
      throws InvalidXMLException {
    var materials = ModernMaterialParser.parseFlatten(node);
    while (count-- > 0) {
      recipe.addIngredient(new RecipeChoice.MaterialChoice(materials));
    }
  }

  @Override
  public void setIngredient(Node node, ShapedRecipe recipe, char key) throws InvalidXMLException {
    var materials = ModernMaterialParser.parseFlatten(node);
    recipe.setIngredient(key, new RecipeChoice.MaterialChoice(materials));
  }

  @Override
  public FurnaceRecipe createFurnaceRecipe(Node input, ItemStack result)
      throws InvalidXMLException {
    var materials = ModernMaterialParser.parseFlatten(input);
    return new FurnaceRecipe(getKey(), result, new RecipeChoice.MaterialChoice(materials), 0, 200);
  }

  @Override
  public void addRecipe(World world, Recipe recipe) {
    Bukkit.addRecipe(recipe);
  }

  @Override
  public void resetRecipes(World world) {
    Bukkit.resetRecipes();
  }
}
