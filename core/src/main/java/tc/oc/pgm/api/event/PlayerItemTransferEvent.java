package tc.oc.pgm.api.event;

import javax.annotation.Nullable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Called when an {@link ItemStack} moves in or out of a {@link Player}'s {@link Inventory}.
 *
 * @see ItemTransferEvent
 */
public class PlayerItemTransferEvent extends ItemTransferEvent {

  private final Player player;
  private final @Nullable ItemStack cursorItem;

  public PlayerItemTransferEvent(
      Event cause,
      Type type,
      Player player,
      @Nullable Inventory fromInventory,
      @Nullable Integer fromSlot,
      @Nullable Inventory toInventory,
      @Nullable Integer toSlot,
      ItemStack itemStack,
      @Nullable Item itemEntity,
      int quantity,
      @Nullable ItemStack cursorItem) {

    super(
        cause, type, fromInventory, fromSlot, toInventory, toSlot, itemStack, itemEntity, quantity);
    this.player = player;
    this.cursorItem = cursorItem;
  }

  /**
   * Get the {@link Player} responsible for the {@link ItemTransferEvent}.
   *
   * @return The responsible {@link Player}.
   */
  public Player getPlayer() {
    return player;
  }

  @Override
  public Player getActor() {
    return getPlayer();
  }

  /**
   * Get the {@link ItemStack} of the {@link Player}'s cursor during the event.
   *
   * @return The cursor {@link ItemStack} of the {@link Player}.
   */
  @Nullable
  public ItemStack getCursorItem() {
    return cursorItem;
  }

  /**
   * Get the quantity of the {@link #getItemStack()} before the event occurred, including the
   * cursor.
   *
   * @param type An {@link ItemStack}, only used to check for {@link
   *     ItemStack#isSimilar(ItemStack)}.
   * @return The prior quantity of the {@link #getItemStack()}.
   */
  public int getPriorQuantity(ItemStack type) {
    int quantity = 0;
    for (ItemStack stack : player.getInventory().getContents()) {
      if (stack != null && stack.isSimilar(type)) {
        quantity += stack.getAmount();
      }
    }
    if (cursorItem != null && cursorItem.isSimilar(type)) {
      quantity += cursorItem.getAmount();
    }
    return quantity;
  }

  /**
   * Get whether the {@link Player} is acquiring the {@link #getItemStack()} into their {@link
   * Inventory}.
   *
   * @return Whether the event is an {@link ItemStack} acquisition.
   */
  public boolean isAcquiring() {
    return getType() == Type.TAKE
        || getType() == Type.PICKUP
        || (getType() == Type.TRANSFER
            && getToInventory() != null
            && getToInventory().getHolder() == player);
  }

  /**
   * Get whether the {@link Player} is relinquishing the {@link #getItemStack()} from their {@link
   * Inventory}.
   *
   * @return Whether the event is an {@link ItemStack} drop.
   */
  public boolean isRelinquishing() {
    return getType() == Type.PLACE
        || getType() == Type.DROP
        || (getType() == Type.TRANSFER
            && getToInventory() != null
            && getToInventory().getHolder() != player);
  }
}
