package tc.oc.pgm.util.event;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

/**
 * An event when an {@link ItemStack} moves in or out of an {@link PlayerInventory}.
 *
 * @see ItemTransferEvent
 */
public class PlayerItemTransferEvent extends ItemTransferEvent {

  private final Player player;
  private final @Nullable ItemStack cursor;

  public PlayerItemTransferEvent(
      final Event cause,
      final Reason type,
      final Player player,
      final @Nullable Inventory fromInventory,
      final @Nullable Inventory toInventory,
      final ItemStack itemStack,
      final @Nullable Item itemEntity,
      final int quantity,
      final @Nullable ItemStack cursor) {
    super(cause, type, fromInventory, toInventory, itemStack, itemEntity, quantity);
    this.player = player;
    this.cursor = cursor;
  }

  /**
   * Gets the {@link Player} that transferred the item.
   *
   * @return a player.
   */
  public Player getPlayer() {
    return player;
  }

  @Override
  public Player getActor() {
    return getPlayer();
  }

  /**
   * Gets the {@link ItemStack} that the player has cursor selected.
   *
   * @return an item stack
   */
  @Nullable
  public ItemStack getCursor() {
    return cursor;
  }

  /**
   * Tests if the player is acquiring the item into their inventory.
   *
   * @return if the event is an acquisition
   */
  public boolean isAcquiring() {
    final Reason reason = this.getReason();
    return reason == Reason.TAKE
        || reason == Reason.PICKUP
        || (reason == Reason.TRANSFER && getTo() != null && getTo().getHolder() == this.player);
  }

  /**
   * Tests if the player is relinquishing the item from their inventory.
   *
   * @return if the event is a relinquishment
   */
  public boolean isRelinquishing() {
    final Reason reason = this.getReason();
    return reason == Reason.PLACE
        || reason == Reason.DROP
        || (reason == Reason.TRANSFER && getTo() != null && getTo().getHolder() != this.player);
  }
}
