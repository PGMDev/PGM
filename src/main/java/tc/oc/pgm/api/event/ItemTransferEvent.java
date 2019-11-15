package tc.oc.pgm.api.event;

import javax.annotation.Nullable;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Called when an {@link ItemStack} moves in or out of an {@link Inventory}. */
public class ItemTransferEvent extends GeneralizingEvent {

  public enum Type {
    PLACE, // Item placed in an inventory through a GUI
    TAKE, // Item taken from an inventory through a GUI
    TRANSFER, // Item transferred instantly from one inventory to another
    PICKUP, // Item picked up from the world
    DROP, // Item dropped into the world
    PLUGIN // Item transferred somehow by a plugin
  }

  private final Type type;
  private final @Nullable Inventory fromInventory;
  private final @Nullable Integer fromSlot;
  private final @Nullable Inventory toInventory;
  private final @Nullable Integer toSlot;
  private final ItemStack itemStack;
  private final @Nullable Item itemEntity;
  private int quantity;

  public ItemTransferEvent(
      @Nullable Event cause,
      Type type,
      @Nullable Inventory fromInventory,
      @Nullable Integer fromSlot,
      @Nullable Inventory toInventory,
      @Nullable Integer toSlot,
      ItemStack itemStack,
      @Nullable Item itemEntity,
      int quantity) {
    super(cause);
    this.type = type;
    this.fromInventory = fromInventory;
    this.fromSlot = fromSlot;
    this.toInventory = toInventory;
    this.toSlot = toSlot;
    this.itemStack = itemStack;
    this.itemEntity = itemEntity;
    this.quantity = quantity;
  }

  /**
   * Get the generic "type" of {@link ItemTransferEvent} this is.
   *
   * @return The {@link Type} of {@link ItemTransferEvent}.
   */
  public Type getType() {
    return type;
  }

  /**
   * Get the {@link ItemStack} that moved from one {@link Inventory} to another.
   *
   * @return The involved {@link ItemStack}.
   */
  public ItemStack getItemStack() {
    return itemStack;
  }

  /**
   * Get the {@link Item} entity that was involved, if any.
   *
   * @return The involved {@link Item}.
   */
  @Nullable
  public Item getItemEntity() {
    return itemEntity;
  }

  /**
   * Get the {@link Inventory} the {@link ItemStack} was previously from.
   *
   * @return The previous {@link Inventory}.
   */
  @Nullable
  public Inventory getFromInventory() {
    return fromInventory;
  }

  /**
   * Get the slot in the {@link Inventory} the {@link ItemStack} was previously from.
   *
   * @return The previous {@link Inventory} slot.
   */
  @Nullable
  public Integer getFromSlot() {
    return fromSlot;
  }

  /**
   * Get the {@link Inventory} the {@link ItemStack} has moved to.
   *
   * @return The new {@link Inventory}.
   */
  @Nullable
  public Inventory getToInventory() {
    return toInventory;
  }

  /**
   * Get the slot in the {@link Inventory} the {@link ItemStack} has moved to.
   *
   * @return The new {@link Inventory} slot.
   */
  @Nullable
  public Integer getToSlot() {
    return toSlot;
  }

  /**
   * Get the quantity of the {@link ItemStack}.
   *
   * @return The quantity of the involved {@link ItemStack}.
   */
  public int getQuantity() {
    return quantity;
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
