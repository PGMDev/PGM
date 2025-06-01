package tc.oc.pgm.util.event.player;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.event.SportPaper;

@SportPaper
public class PlayerSpawnEntityEvent extends PlayerEvent {
  private static final HandlerList handlers = new HandlerList();
  private final Entity what;
  private final ItemStack item;

  public PlayerSpawnEntityEvent(final Player who, final Entity what, final ItemStack item) {
    super(who);
    this.what = what;
    this.item = item;
  }

  /**
   * Gets the entity the player is spawning.
   *
   * @return the entity the player is spawning
   */
  public Entity getEntity() {
    return what;
  }

  /**
   * Gets the item that is being used to spawn an entity. Modifying the returned item will have no
   * effect.
   *
   * @return an ItemStack for the item being used
   */
  public ItemStack getItem() {
    return item.clone();
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
