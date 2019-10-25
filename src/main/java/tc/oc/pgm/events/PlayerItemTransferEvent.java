package tc.oc.pgm.events;

import javax.annotation.Nullable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerItemTransferEvent extends ItemTransferEvent {
  private final Player player;
  @Nullable protected final ItemStack cursorItems;

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
      @Nullable ItemStack cursorItems) {

    super(
        cause, type, fromInventory, fromSlot, toInventory, toSlot, itemStack, itemEntity, quantity);
    this.player = player;
    this.cursorItems = cursorItems;
  }

  @Override
  public String toString() {
    String s = super.toString();

    s += " player=" + this.player.getName();

    if (this.cursorItems != null) {
      s += " cursor=" + this.cursorItems;
    }

    return s;
  }

  public Player getPlayer() {
    return player;
  }

  @Override
  public Player getActor() {
    return getPlayer();
  }

  @Nullable
  public ItemStack getCursorItems() {
    return cursorItems;
  }

  /**
   * Return the quantity of items stackable with the given item that the player was in posession of
   * prior to the transfer event. This includes any items being carried on the cursor.
   */
  public int getPriorQuantity(ItemStack type) {
    int quantity = 0;
    for (ItemStack stack : this.player.getInventory().getContents()) {
      if (stack != null && stack.isSimilar(type)) {
        quantity += stack.getAmount();
      }
    }
    if (this.cursorItems != null && this.cursorItems.isSimilar(type)) {
      quantity += this.cursorItems.getAmount();
    }
    return quantity;
  }

  /** Equivalent to getPriorQuantity(getItemStack()) */
  public int getPriorQuantity() {
    return this.getPriorQuantity(this.itemStack);
  }

  public boolean isAcquiring() {
    return this.type == Type.TAKE
        || this.type == Type.PICKUP
        || (this.type == Type.TRANSFER
            && this.toInventory != null
            && this.toInventory.getHolder() == this.player);
  }

  public boolean isRelinquishing() {
    return this.type == Type.PLACE
        || this.type == Type.DROP
        || (this.type == Type.TRANSFER
            && this.toInventory != null
            && this.toInventory.getHolder() != this.player);
  }
}
