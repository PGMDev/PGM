package tc.oc.pgm.crafting;

import static tc.oc.pgm.util.bukkit.RecipeUtils.RECIPE_UTILS;

import java.util.Set;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.material.MaterialMatcher;

public class CraftingMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Set<Recipe> customRecipes;
  private final MaterialMatcher disabledRecipes;

  public CraftingMatchModule(
      Match match, Set<Recipe> customRecipes, MaterialMatcher disabledRecipes) {
    this.match = match;
    this.customRecipes = customRecipes;
    this.disabledRecipes = disabledRecipes;
  }

  @Override
  public void enable() {
    World world = match.getWorld();
    for (Recipe recipe : customRecipes) {
      RECIPE_UTILS.addRecipe(world, recipe);
    }
  }

  @Override
  public void disable() {
    // Recipe changes affect all worlds on the server, so we make changes at match start/end
    // to avoid interfering with adjacent matches. If we wait until unload() to reset them,
    // the next match would already be loaded.
    RECIPE_UTILS.resetRecipes(match.getWorld());
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void removeDisabledRecipe(PrepareItemCraftEvent event) {
    CraftingInventory crafting = event.getInventory();
    ItemStack result = crafting.getResult();
    if (result == null) {
      return;
    }

    if (disabledRecipes.matches(result)) crafting.setResult(null);
  }
}
