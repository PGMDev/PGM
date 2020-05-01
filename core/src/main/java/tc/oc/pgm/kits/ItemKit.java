package tc.oc.pgm.kits;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.InventoryUtils;

public class ItemKit implements KitDefinition {

  protected final ImmutableMap<Slot, ItemStack> slotItems;
  protected final ImmutableList<ItemStack> freeItems;

  public ItemKit(Map<Slot, ItemStack> slotItems, List<ItemStack> freeItems) {
    this.slotItems = ImmutableMap.copyOf(slotItems);
    this.freeItems = ImmutableList.copyOf(freeItems);
  }

  public ImmutableMap<Slot, ItemStack> getSlotItems() {
    return slotItems;
  }

  public ImmutableList<ItemStack> getFreeItems() {
    return freeItems;
  }

  public Iterable<ItemStack> getItems() {
    return Iterables.concat(slotItems.values(), freeItems);
  }

  /**
   * If force is true, the kit will replace any items in the player's inventory that conflict with
   * the kit, otherwise a smart algorithm is used to fill existing stacks and repair tools.
   *
   * <p>TODO: Ender chest support - {@link Slot} already supports it, but the stack merging code in
   * this method does not.
   */
  @Override
  public void apply(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    ApplyItemKitEvent event = new ApplyItemKitEvent(player, this);
    player.getMatch().callEvent(event);
    if (event.isCancelled()) {
      return;
    }

    final HumanEntity holder = player.getBukkit();
    final PlayerInventory inv = player.getBukkit().getInventory();

    if (force) {
      for (Entry<Slot, ItemStack> kitEntry : event.getSlotItems().entrySet()) {
        kitEntry.getKey().putItem(holder, kitEntry.getValue().clone());
      }
    } else {
      // Tools in the player's inv are repaired using matching tools in the kit with less damage
      for (ItemStack kitStack : event.getItems()) {
        for (ItemStack invStack : inv.getContents()) {
          if (invStack != null) {
            if (kitStack.getAmount() > 0
                && kitStack.getType().getMaxDurability() > 0
                && kitStack.getType().equals(invStack.getType())
                && kitStack.getEnchantments().equals(invStack.getEnchantments())
                && kitStack.getDurability() < invStack.getDurability()) {

              invStack.setDurability(kitStack.getDurability());
              kitStack.setAmount(0);
              break;
            }
          }
        }
      }

      // Items in the player's inv that stack with kit items are deducted from the kit
      for (ItemStack invStackOrig : inv.getContents()) {
        if (invStackOrig != null) {
          ItemStack invStack = invStackOrig.clone();
          for (ItemStack kitStack : event.getItems()) {
            if (kitStack.isSimilar(invStack)) {
              int reduce = Math.min(invStack.getAmount(), kitStack.getAmount());
              if (reduce > 0) {
                invStack.setAmount(invStack.getAmount() - reduce);
                kitStack.setAmount(kitStack.getAmount() - reduce);
              }
            }
          }
        }
      }

      // Fill partial stacks of kit items that are already in the player's inv.
      // We must do this in a seperate pass so that kit stacks don't combine with
      // other kit stacks.
      for (ItemStack kitStack : event.getItems()) {
        for (ItemStack invStack : inv.getContents()) {
          if (invStack != null && kitStack.isSimilar(invStack)) {
            int transfer =
                Math.min(kitStack.getAmount(), invStack.getMaxStackSize() - invStack.getAmount());
            if (transfer > 0) {
              kitStack.setAmount(kitStack.getAmount() - transfer);
              invStack.setAmount(invStack.getAmount() + transfer);
            }
          }
        }
      }

      // Put the remaining kit slotted items into their designated inv slots.
      // If a slot is occupied, add the stack to displacedStacks.
      for (Entry<Slot, ItemStack> kitEntry : event.getSlotItems().entrySet()) {
        Slot kitSlot = kitEntry.getKey();
        ItemStack kitStack = kitEntry.getValue();

        if (kitStack.getAmount() > 0) {
          if (InventoryUtils.isNothing(kitSlot.getItem(holder))) {
            kitSlot.putItem(holder, kitStack);
          } else {
            displacedItems.add(kitStack);
          }
        }
      }
    }

    // Add the kit's free items to displacedItems
    displacedItems.addAll(event.getFreeItems());
  }

  @Override
  public boolean isRemovable() {
    return false;
  }

  @Override
  public void remove(MatchPlayer player) {
    throw new UnsupportedOperationException(this + " is not removable");
  }
}
