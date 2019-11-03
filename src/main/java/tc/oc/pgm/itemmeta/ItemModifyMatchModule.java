package tc.oc.pgm.itemmeta;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.match.MatchModule;

@ListenerScope(MatchScope.LOADED)
public class ItemModifyMatchModule extends MatchModule implements Listener {

  private final ItemModifyModule imm;

  public ItemModifyMatchModule(Match match) {
    super(match);
    this.imm = match.getMapContext().needModule(ItemModifyModule.class);
  }

  private boolean applyRules(ItemStack stack) {
    return imm.applyRules(stack);
  }

  @EventHandler
  public void onItemSpawn(ItemSpawnEvent event) {
    ItemStack stack = event.getEntity().getItemStack();
    if (applyRules(stack)) {
      event.getEntity().setItemStack(stack);
    }
  }

  @EventHandler
  public void onItemCraft(CraftItemEvent event) {
    ItemStack stack = event.getCurrentItem();
    if (applyRules(stack)) {
      event.setCurrentItem(stack);
      event.getInventory().setResult(stack);
    }
  }

  @EventHandler
  public void onPrepareItemCraft(PrepareItemCraftEvent event) {
    ItemStack stack = event.getInventory().getResult();
    if (applyRules(stack)) {
      event.getInventory().setResult(stack);
    }
  }

  @EventHandler
  public void onInventoryOpen(InventoryOpenEvent event) {
    ItemStack[] contents = event.getInventory().getContents();
    for (int i = 0; i < contents.length; i++) {
      if (applyRules(contents[i])) {
        event.getInventory().setItem(i, contents[i]);
      }
    }
  }

  @EventHandler
  public void onArmorDispense(BlockDispenseEvent event) {
    // This covers armor being equipped by a dispenser, which does not call any of the other events
    ItemStack stack = event.getItem();
    if (applyRules(stack)) {
      event.setItem(stack);
    }
  }
}
