package tc.oc.pgm.kits;

import com.google.common.collect.Iterables;
import java.util.*;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.Slot;

/**
 * Fired when an {@link ItemKit} is applied to a player. The kit can be modified through the
 * containers returned by the various getter methods. Note that {@link ArmorKit}s fire a generic
 * {@link ApplyKitEvent}, not this one.
 */
public class ApplyItemKitEvent extends ApplyKitEvent {
  private final Map<Slot, ItemStack> slotItems;
  private final List<ItemStack> freeItems;
  private final List<ItemStack> displacedItems;

  public ApplyItemKitEvent(
      MatchPlayer player, ItemKit kit, boolean force, List<ItemStack> displacedItems) {
    super(player, kit, force);

    this.slotItems = new HashMap<>(kit.getSlotItems().size());
    for (Map.Entry<Slot, ItemStack> entry : kit.getSlotItems().entrySet()) {
      this.slotItems.put(entry.getKey(), entry.getValue().clone());
    }

    this.freeItems = new ArrayList<>(kit.getFreeItems().size());
    for (ItemStack stack : kit.getFreeItems()) {
      this.freeItems.add(stack.clone());
    }
    this.displacedItems = displacedItems;
  }

  /**
   * Return a map of the items in the kit by slot number. This map, and the contained ItemStacks,
   * can be modified to alter what is applied, without altering the original kit.
   */
  public Map<Slot, ItemStack> getSlotItems() {
    return slotItems;
  }

  public List<ItemStack> getFreeItems() {
    return freeItems;
  }

  public List<ItemStack> getDisplacedItems() {
    return displacedItems;
  }

  /**
   * Return all items that will be applied by the kit. Iterators from the returned iterable support
   * {@link Iterator#remove} to prevent the item from being applied, without modifying the original
   * kit.
   */
  public Iterable<ItemStack> getItems() {
    return Iterables.concat(slotItems.values(), freeItems);
  }

  @Override
  public ItemKit getKit() {
    return (ItemKit) super.getKit();
  }
}
