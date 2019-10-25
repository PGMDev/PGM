package tc.oc.pgm.events;

import javax.annotation.Nullable;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Fired when an item moves in/out of an Inventory */
public class ItemTransferEvent extends GeneralizingEvent {
  public static enum Type {
    PLACE, // Item placed in an inventory through a GUI
    TAKE, // Item taken from an inventory through a GUI
    TRANSFER, // Item transferred instantly from one inventory to another
    PICKUP, // Item picked up from the world
    DROP, // Item dropped into the world
    PLUGIN // Item transferred somehow by a plugin
  }

  protected final Type type;
  @Nullable protected final Inventory fromInventory;
  @Nullable protected final Integer fromSlot;
  @Nullable protected final Inventory toInventory;
  @Nullable protected final Integer toSlot;
  protected final ItemStack itemStack;
  @Nullable protected final Item itemEntity;
  protected int quantity;

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

  @Override
  public String toString() {
    String s =
        this.getClass().getName() + " cause=" + this.cause.getEventName() + " type=" + this.type;

    if (this.fromInventory != null) {
      s += " from=" + this.fromInventory.getName();
      if (this.fromSlot != null) {
        s += ":" + this.fromSlot;
      }
    }

    if (this.toInventory != null) {
      s += " to=" + this.toInventory.getName();
      if (this.toSlot != null) {
        s += ":" + this.toSlot;
      }
    }

    if (this.itemStack != null) {
      s += " stack=" + this.itemStack;
    }

    if (this.itemEntity != null) {
      s += " entity=" + this.itemEntity;
    }

    return s + " qty=" + this.quantity;
  }

  public Type getType() {
    return type;
  }

  @Nullable
  public Inventory getFromInventory() {
    return fromInventory;
  }

  @Nullable
  public Integer getFromSlot() {
    return fromSlot;
  }

  @Nullable
  public Inventory getToInventory() {
    return toInventory;
  }

  @Nullable
  public Integer getToSlot() {
    return toSlot;
  }

  public ItemStack getItemStack() {
    return itemStack;
  }

  @Nullable
  public Item getItemEntity() {
    return itemEntity;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
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
