package tc.oc.pgm.util.event;

import lombok.Getter;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

/** An event when an {@link ItemStack} moves in or out of an {@link Inventory}. */
public class ItemTransferEvent extends GeneralizedEvent {

  /** A reason why an {@link ItemStack} was transferred. */
  public enum Reason {
    PLACE, // Item placed in an inventory through a GUI
    TAKE, // Item taken from an inventory through a GUI
    TRANSFER, // Item transferred instantly from one inventory to another
    PICKUP, // Item picked up from the world
    DROP, // Item dropped into the world
    PLUGIN // Item transferred somehow by a plugin
  }

  /** The reason for the item transfer. */
  @Getter
  private final Reason reason;

  /** The {@link ItemStack} that was transferred. */
  @Getter
  private final ItemStack item;

  private final @Nullable Item entity;

  /** The {@link ItemStack} quantity. */
  @Getter
  private final int quantity;

  private final @Nullable Inventory from;
  private final @Nullable Inventory to;

  public ItemTransferEvent(
      final @Nullable Event cause,
      final Reason reason,
      final @Nullable Inventory from,
      final @Nullable Inventory to,
      final ItemStack item,
      final @Nullable Item entity,
      final int quantity) {
    super(cause);
    this.reason = reason;
    this.item = item;
    this.entity = entity;
    this.quantity = quantity;
    this.from = from;
    this.to = to;
  }

  /**
   * Gets the {@link Item} entity, if the item was dropped.
   *
   * @return an item entity
   */
  @Nullable
  public Item getEntity() {
    return this.entity;
  }

  /**
   * Gets the {@link Inventory} where the item originated.
   *
   * @return an inventory
   */
  @Nullable
  public Inventory getFrom() {
    return this.from;
  }

  /**
   * Gets the {@link Inventory} where the item was transferred.
   *
   * @return an inventory
   */
  @Nullable
  public Inventory getTo() {
    return this.to;
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
