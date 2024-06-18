package tc.oc.pgm.util.bukkit;

import org.bukkit.World;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public interface RecipeUtils {
  RecipeUtils RECIPE_UTILS = Platform.get(RecipeUtils.class);

  ShapedRecipe createShapedRecipe(ItemStack result);

  ShapelessRecipe createShapelessRecipe(ItemStack result);

  FurnaceRecipe createFurnaceRecipe(Node ingredient, ItemStack result) throws InvalidXMLException;

  void addIngredient(Node ingredient, ShapelessRecipe recipe, int count) throws InvalidXMLException;

  void setIngredient(Node ingredient, ShapedRecipe recipe, char key) throws InvalidXMLException;

  void addRecipe(World world, Recipe recipe);

  void resetRecipes(World world);
}
