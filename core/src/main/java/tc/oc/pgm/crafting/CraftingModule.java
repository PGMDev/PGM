package tc.oc.pgm.crafting;

import static tc.oc.pgm.util.material.MaterialUtils.MATERIAL_UTILS;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class CraftingModule implements MapModule<CraftingMatchModule> {

  private final Set<Recipe> customRecipes;
  private final MaterialMatcher disabledRecipes;

  public CraftingModule(Set<Recipe> customRecipes, MaterialMatcher disabledRecipes) {
    this.customRecipes = ImmutableSet.copyOf(customRecipes);
    this.disabledRecipes = disabledRecipes;
  }

  @Override
  public CraftingMatchModule createMatchModule(Match match) {
    return new CraftingMatchModule(match, customRecipes, disabledRecipes);
  }

  public static class Factory implements MapModuleFactory<CraftingModule> {
    @Override
    public @Nullable CraftingModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Set<Recipe> customRecipes = new HashSet<>();
      MaterialMatcher.Builder disabledRecipes = MaterialMatcher.builder();

      for (Element elCrafting : doc.getRootElement().getChildren("crafting")) {
        for (Element elDisable : elCrafting.getChildren("disable")) {
          disabledRecipes.parse(elDisable);
        }

        for (Element elRecipe : XMLUtils.getChildren(elCrafting, "shapeless", "shaped", "smelt")) {
          Recipe recipe;
          switch (elRecipe.getName()) {
            case "shapeless":
              recipe = parseShapelessRecipe(factory, elRecipe);
              break;

            case "shaped":
              recipe = parseShapedRecipe(factory, elRecipe);
              break;

            case "smelt":
              recipe = parseSmeltingRecipe(factory, elRecipe);
              break;

            default:
              throw new IllegalStateException();
          }

          customRecipes.add(recipe);
          if (XMLUtils.parseBoolean(elRecipe.getAttribute("override"), false)) {
            // Disable specific world + data
            disabledRecipes.add(recipe.getResult(), false);
          } else if (XMLUtils.parseBoolean(elRecipe.getAttribute("override-all"), false)) {
            // Disable all of this world
            disabledRecipes.add(recipe.getResult().getType(), true);
          }
        }
      }

      return customRecipes.isEmpty() && disabledRecipes.isEmpty()
          ? null
          : new CraftingModule(customRecipes, disabledRecipes.build());
    }

    private ItemStack parseRecipeResult(MapFactory factory, Element elRecipe)
        throws InvalidXMLException {
      return factory
          .getKits()
          .parseItem(XMLUtils.getRequiredUniqueChild(elRecipe, "result"), false);
    }

    public Recipe parseShapelessRecipe(MapFactory factory, Element elRecipe)
        throws InvalidXMLException {
      ShapelessRecipe recipe = new ShapelessRecipe(parseRecipeResult(factory, elRecipe));

      for (Element elIngredient : XMLUtils.getChildren(elRecipe, "ingredient", "i")) {
        int count = XMLUtils.parseNumber(elIngredient.getAttribute("amount"), Integer.class, 1);
        MATERIAL_UTILS.addIngredient(new Node(elIngredient), recipe, count);
      }

      if (recipe.getIngredientList().isEmpty()) {
        throw new InvalidXMLException(
            "Crafting recipe must have at least one ingredient", elRecipe);
      }

      return recipe;
    }

    public Recipe parseShapedRecipe(MapFactory factory, Element elRecipe)
        throws InvalidXMLException {
      ShapedRecipe recipe = new ShapedRecipe(parseRecipeResult(factory, elRecipe));

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
      Set<Character> keys = recipe
          .getIngredientMap()
          .keySet(); // All shape symbols are present and mapped to null at this point

      for (Element elIngredient : elRecipe.getChildren("ingredient")) {
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
        MATERIAL_UTILS.setIngredient(new Node(elIngredient), recipe, key);
      }

      if (recipe.getIngredientMap().isEmpty()) {
        throw new InvalidXMLException(
            "Crafting recipe must have at least one ingredient", elRecipe);
      }

      return recipe;
    }

    public Recipe parseSmeltingRecipe(MapFactory factory, Element elRecipe)
        throws InvalidXMLException {
      ItemStack result = parseRecipeResult(factory, elRecipe);
      return MATERIAL_UTILS.createFurnaceRecipe(
          new Node(XMLUtils.getRequiredUniqueChild(elRecipe, "ingredient", "i")), result);
    }
  }
}
