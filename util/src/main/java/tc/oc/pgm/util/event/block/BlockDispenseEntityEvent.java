package tc.oc.pgm.util.event.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.event.SportPaper;

/** Called when an entity is dispensed from a block. */
@SportPaper
public class BlockDispenseEntityEvent extends BlockEvent {
  private ItemStack item;
  private Vector velocity;
  private final Entity entity;

  public BlockDispenseEntityEvent(
      final Block block, final ItemStack dispensed, final Vector velocity, final Entity entity) {
    super(block);
    this.item = dispensed;
    this.velocity = velocity;
    this.entity = entity;
  }

  public ItemStack getItem() {
    return this.item.clone();
  }

  public void setItem(ItemStack item) {
    this.item = item;
  }

  public Vector getVelocity() {
    return this.velocity.clone();
  }

  public void setVelocity(Vector vel) {
    this.velocity = vel;
  }

  /**
   * Gets the entity that is being dispensed.
   *
   * @return An Entity for the item being dispensed
   */
  public Entity getEntity() {
    return entity;
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
