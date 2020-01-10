package tc.oc.pgm.crafting;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

public class CraftingModule implements MapModule {

  private final Set<Recipe> customRecipes;
  private final Set<SingleMaterialMatcher> disabledRecipes;

  public CraftingModule(Set<Recipe> customRecipes, Set<SingleMaterialMatcher> disabledRecipes) {
    this.customRecipes = ImmutableSet.copyOf(customRecipes);
    this.disabledRecipes = ImmutableSet.copyOf(disabledRecipes);
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new CraftingMatchModule(match, customRecipes, disabledRecipes);
  }

  public static class Factory implements MapModuleFactory<CraftingModule> {
    @Override
    public @Nullable CraftingModule parse(MapContext context, Logger logger, Document doc)
        throws InvalidXMLException {
      Set<Recipe> customRecipes = new HashSet<>();
      Set<SingleMaterialMatcher> disabledRecipes = new HashSet<>();

      for (Element elCrafting : doc.getRootElement().getChildren("crafting")) {
        for (Element elDisable : elCrafting.getChildren("disable")) {
          disabledRecipes.add(XMLUtils.parseMaterialPattern(elDisable));
        }

        for (Element elRecipe : XMLUtils.getChildren(elCrafting, "shapeless", "shaped", "smelt")) {
          Recipe recipe;
          switch (elRecipe.getName()) {
            case "shapeless":
              recipe = parseShapelessRecipe(context, elRecipe);
              break;

            case "shaped":
              recipe = parseShapedRecipe(context, elRecipe);
              break;

            case "smelt":
              recipe = parseSmeltingRecipe(context, elRecipe);
              break;

            default:
              throw new IllegalStateException();
          }

          customRecipes.add(recipe);
          if (XMLUtils.parseBoolean(elRecipe.getAttribute("override"), false)) {
            // Disable specific world + data
            disabledRecipes.add(new SingleMaterialMatcher(recipe.getResult().getData()));
          } else if (XMLUtils.parseBoolean(elRecipe.getAttribute("override-all"), false)) {
            // Disable all of this world
            disabledRecipes.add(new SingleMaterialMatcher(recipe.getResult().getType()));
          }
        }
      }

      return customRecipes.isEmpty() && disabledRecipes.isEmpty()
          ? null
          : new CraftingModule(customRecipes, disabledRecipes);
    }

    private ItemStack parseRecipeResult(MapContext context, Element elRecipe)
        throws InvalidXMLException {
      return context
          .legacy()
          .getKits()
          .parseItem(XMLUtils.getRequiredUniqueChild(elRecipe, "result"), false);
    }

    public Recipe parseShapelessRecipe(MapContext context, Element elRecipe)
        throws InvalidXMLException {
      ShapelessRecipe recipe = new ShapelessRecipe(parseRecipeResult(context, elRecipe));

      for (Element elIngredient : XMLUtils.getChildren(elRecipe, "ingredient", "i")) {
        SingleMaterialMatcher item = XMLUtils.parseMaterialPattern(elIngredient);
        int count = XMLUtils.parseNumber(elIngredient.getAttribute("amount"), Integer.class, 1);
        if (item.dataMatters()) {
          recipe.addIngredient(count, item.getMaterialData());
        } else {
          recipe.addIngredient(count, item.getMaterial());
        }
      }

      if (recipe.getIngredientList().isEmpty()) {
        throw new InvalidXMLException(
            "Crafting recipe must have at least one ingredient", elRecipe);
      }

      return recipe;
    }

    public Recipe parseShapedRecipe(MapContext context, Element elRecipe)
        throws InvalidXMLException {
      ShapedRecipe recipe = new ShapedRecipe(parseRecipeResult(context, elRecipe));

      Element elShape = XMLUtils.getRequiredUniqueChild(elRecipe, "shape");
      List<String> rows = new ArrayList<>(3);

      for (Element elRow : elShape.getChildren("row")) {
        String row = elRow.getTextNormalize();

        if (rows.size() >= 3) {
          throw new InvalidXMLException(
              "Shape must have no more than 3 rows (" + row + ")", elShape);
        }

        if (rows.isEmpty()) {
          if (row.length() > 3) {
            throw new InvalidXMLException(
                "Shape must have no more than 3 columns (" + row + ")", elShape);
          }
        } else if (row.length() != rows.get(0).length()) {
          throw new InvalidXMLException("All rows must be the same width", elShape);
        }

        rows.add(row);
      }

      if (rows.isEmpty()) {
        throw new InvalidXMLException("Shape must have at least one row", elShape);
      }

      recipe.shape(rows.toArray(new String[rows.size()]));
      Set<Character> keys =
          recipe
              .getIngredientMap()
              .keySet(); // All shape symbols are present and mapped to null at this point

      for (Element elIngredient : elRecipe.getChildren("ingredient")) {
        SingleMaterialMatcher item = XMLUtils.parseMaterialPattern(elIngredient);
        Attribute attrSymbol = XMLUtils.getRequiredAttribute(elIngredient, "symbol");
        String symbol = attrSymbol.getValue();

        if (symbol.length() != 1) {
          throw new InvalidXMLException(
              "Ingredient key must be a single character from the recipe shape", attrSymbol);
        }

        char key = symbol.charAt(0);
        if (!keys.contains(key)) {
          throw new InvalidXMLException(
              "Ingredient key '" + key + "' does not appear in the recipe shape", attrSymbol);
        }

        if (item.dataMatters()) {
          recipe.setIngredient(key, item.getMaterialData());
        } else {
          recipe.setIngredient(key, item.getMaterial());
        }
      }

      if (recipe.getIngredientMap().isEmpty()) {
        throw new InvalidXMLException(
            "Crafting recipe must have at least one ingredient", elRecipe);
      }

      return recipe;
    }

    public Recipe parseSmeltingRecipe(MapContext context, Element elRecipe)
        throws InvalidXMLException {
      SingleMaterialMatcher ingredient =
          XMLUtils.parseMaterialPattern(
              XMLUtils.getRequiredUniqueChild(elRecipe, "ingredient", "i"));
      ItemStack result = parseRecipeResult(context, elRecipe);
      if (ingredient.dataMatters()) {
        return new FurnaceRecipe(result, ingredient.getMaterialData());
      } else {
        return new FurnaceRecipe(result, ingredient.getMaterial());
      }
    }
  }
}
