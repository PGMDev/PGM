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
import tc.oc.pgm.kits.tag.ItemModifier;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.inventory.Slot;

public class ItemKit implements KitDefinition {
  public static final int INFINITE_STACK_SIZE = 99;

  protected final ImmutableMap<Slot, ItemStack> slotItems;
  protected final ImmutableList<ItemStack> freeItems;
  protected final boolean repairTools;
  protected final boolean deductTools;
  protected final boolean deductItems;
  protected final boolean dropOverflow;

  public ItemKit(Map<Slot, ItemStack> slotItems, List<ItemStack> freeItems) {
    this(slotItems, freeItems, true, true, true, false);
  }

  public ItemKit(
      Map<Slot, ItemStack> slotItems,
      List<ItemStack> freeItems,
      boolean repairTools,
      boolean deductTools,
      boolean deductItems,
      boolean dropOverflow) {
    this.slotItems = slotItems == null ? ImmutableMap.of() : ImmutableMap.copyOf(slotItems);
    this.freeItems = freeItems == null ? ImmutableList.of() : ImmutableList.copyOf(freeItems);
    this.repairTools = repairTools;
    this.deductTools = deductTools;
    this.deductItems = deductItems;
    this.dropOverflow = dropOverflow;
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
    ApplyItemKitEvent event = new ApplyItemKitEvent(player, this, force, displacedItems);
    player.getMatch().callEvent(event);
    if (event.isCancelled()) {
      return;
    }

    final HumanEntity holder = player.getBukkit();
    final PlayerInventory inv = player.getBukkit().getInventory();

    // Apply all item modifications (eg: team-colors)
    for (ItemStack item : event.getItems()) {
      ItemModifier.apply(item, player);
    }

    if (force) {
      for (Entry<Slot, ItemStack> kitEntry : event.getSlotItems().entrySet()) {
        kitEntry.getKey().putItem(holder, kitEntry.getValue().clone());
      }
    } else {
      // Tools in the player's inv are repaired using matching tools in the kit with less damage
      if (repairTools) {
        for (ItemStack kitStack : event.getItems()) {
          for (ItemStack invStack : inv.getContents()) {
            if (invStack == null) continue;
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
      if (deductTools || deductItems) {
        for (ItemStack invStack : inv.getContents()) {
          if (invStack == null) continue;
          boolean deduct = invStack.getType().getMaxDurability() > 0 ? deductTools : deductItems;
          if (!deduct) continue;

          int maxAmount = invStack.getAmount();
          for (ItemStack kitStack : event.getItems()) {
            if (!kitStack.isSimilar(invStack)) continue;
            int reduce = Math.min(maxAmount, kitStack.getAmount());
            if (reduce > 0) {
              kitStack.setAmount(kitStack.getAmount() - reduce);
              if ((maxAmount -= reduce) <= 0) break;
            }
          }
        }
      }

      // Fill partial stacks of kit items that are already in the player's inv.
      // We must do this in a separate pass so that kit stacks don't combine with
      // other kit stacks.
      for (ItemStack kitStack : event.getItems()) {
        for (ItemStack invStack : inv.getContents()) {
          if (invStack == null || !kitStack.isSimilar(invStack)) continue;
          int transfer =
              Math.min(kitStack.getAmount(), invStack.getMaxStackSize() - invStack.getAmount());
          if (transfer > 0) {
            kitStack.setAmount(kitStack.getAmount() - transfer);
            invStack.setAmount(invStack.getAmount() + transfer);
          }
        }
      }

      // Put the remaining kit slotted items into their designated inv slots.
      // If a slot is occupied, add the stack to displacedStacks.
      for (Entry<Slot, ItemStack> kitEntry : event.getSlotItems().entrySet()) {
        ItemStack kitStack = kitEntry.getValue();
        if (kitStack.getAmount() <= 0) continue;

        Slot kitSlot = kitEntry.getKey();
        if (InventoryUtils.isNothing(kitSlot.getItem(holder))) {
          kitSlot.putItem(holder, kitStack);
        } else {
          displacedItems.add(kitStack);
        }
      }
    }

    // Add the kit's free items to displacedItems
    displacedItems.addAll(event.getFreeItems());
  }

  @Override
  public void applyLeftover(MatchPlayer player, List<ItemStack> leftover) {
    if (!dropOverflow || leftover.isEmpty()) return;
    for (ItemStack item : leftover) player.getWorld().dropItemNaturally(player.getLocation(), item);
    leftover.clear();
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
