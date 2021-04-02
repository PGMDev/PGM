package tc.oc.pgm.crafting;

import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;

public class CraftingMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Set<Recipe> customRecipes;
  private final Set<SingleMaterialMatcher> disabledRecipes;

  public CraftingMatchModule(
      Match match, Set<Recipe> customRecipes, Set<SingleMaterialMatcher> disabledRecipes) {
    this.match = match;
    this.customRecipes = customRecipes;
    this.disabledRecipes = disabledRecipes;
  }

  @Override
  public void enable() {
    if (BukkitUtils.isSportPaper()) {
      for (Recipe recipe : customRecipes) {
        match.getWorld().addRecipe(recipe);
      }
    } else {
      for (Recipe recipe : customRecipes) {
        Bukkit.addRecipe(recipe);
      }
    }
  }

  @Override
  public void disable() {
    // Recipe changes affect all worlds on the server, so we make changes at match start/end
    // to avoid interfering with adjacent matches. If we wait until unload() to reset them,
    // the next match would already be loaded.
    if (BukkitUtils.isSportPaper()) {
      match.getWorld().resetRecipes();
    } else {
      Bukkit.resetRecipes();
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void removeDisabledRecipe(PrepareItemCraftEvent event) {
    CraftingInventory crafting = event.getInventory();
    ItemStack result = crafting.getResult();
    if (result == null) {
      return;
    }

    for (SingleMaterialMatcher disabled : disabledRecipes) {
      if (disabled.matches(result)) {
        crafting.setResult(null);
        break;
      }
    }
  }
}
