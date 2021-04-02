package tc.oc.pgm.crafting;

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
    World world = match.getWorld();
    for (Recipe recipe : customRecipes) {
      BukkitUtils.addRecipe(world, recipe);
    }
  }

  @Override
  public void disable() {
    // Recipe changes affect all worlds on the server, so we make changes at match start/end
    // to avoid interfering with adjacent matches. If we wait until unload() to reset them,
    // the next match would already be loaded.
    BukkitUtils.resetRecipes(match.getWorld());
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
