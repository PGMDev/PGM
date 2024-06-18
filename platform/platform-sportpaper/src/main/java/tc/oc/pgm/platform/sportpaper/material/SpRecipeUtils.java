package tc.oc.pgm.platform.sportpaper.material;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import org.bukkit.World;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import tc.oc.pgm.util.bukkit.RecipeUtils;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

@Supports(SPORTPAPER)
public class SpRecipeUtils implements RecipeUtils {

  @Override
  public ShapedRecipe createShapedRecipe(ItemStack result) {
    return new ShapedRecipe(result);
  }

  @Override
  public ShapelessRecipe createShapelessRecipe(ItemStack result) {
    return new ShapelessRecipe(result);
  }

  @Override
  public FurnaceRecipe createFurnaceRecipe(Node ingredient, ItemStack result)
      throws InvalidXMLException {
    return new FurnaceRecipe(result, SpMaterialParser.parseBukkit(ingredient));
  }

  @Override
  public void addIngredient(Node ingredient, ShapelessRecipe recipe, int count)
      throws InvalidXMLException {
    recipe.addIngredient(count, SpMaterialParser.parseBukkit(ingredient));
  }

  @Override
  public void setIngredient(Node ingredient, ShapedRecipe recipe, char key)
      throws InvalidXMLException {
    recipe.setIngredient(key, SpMaterialParser.parseBukkit(ingredient));
  }

  @Override
  public void addRecipe(World world, Recipe recipe) {
    world.addRecipe(recipe);
  }

  @Override
  public void resetRecipes(World world) {
    world.resetRecipes();
  }
}
