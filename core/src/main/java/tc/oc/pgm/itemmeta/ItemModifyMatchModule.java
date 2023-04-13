package tc.oc.pgm.itemmeta;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.nms.NMSHacks;

@ListenerScope(MatchScope.LOADED)
public class ItemModifyMatchModule implements MatchModule, Listener {

  private final ItemModifyModule imm;

  public ItemModifyMatchModule(Match match, ItemModifyModule imm) {
    this.imm = imm;
  }

  /**
   * Apply {@link ItemRule}s to the given item stack if applicable.
   *
   * @return {@code true} if any rules were applied to the given item stack, {@code false} if not
   */
  public boolean applyRules(ItemStack stack) {
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

  @EventHandler(ignoreCancelled = true)
  public void onItemPickup(PlayerPickupItemEvent event) {
    // Needed for players picking up arrows stuck in blocks
    if (!NMSHacks.isCraftItemArrowEntity(event.getItem())) return;

    final Item item = event.getItem();
    final ItemStack itemStack = item.getItemStack();

    if (applyRules(itemStack)) {
      event.setCancelled(true);
      NMSHacks.fakePlayerItemPickup(event.getPlayer(), item);
      event.getPlayer().getInventory().addItem(itemStack);
    }
  }
}
