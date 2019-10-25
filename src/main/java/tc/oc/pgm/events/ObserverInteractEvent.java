package tc.oc.pgm.events;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.match.MatchPlayer;

/**
 * Fired when a non-interacting player clicks anywhere. When they click on an entity, this event
 * will fire twice, the second time with the entity set to null. This is a quirk caused by the
 * Bukkit events that generate this one.
 *
 * <p>Note that players on the death screen can generate this event, so if you don't want clicks
 * from dead players, you need to filter them out yourself.
 */
public class ObserverInteractEvent extends MatchPlayerEvent implements Cancellable {

  private final ClickType clickType;
  private final @Nullable Block clickedBlock;
  private final @Nullable Entity clickedEntity;
  private final @Nullable ItemStack clickedItem;

  private boolean cancelled;

  public ObserverInteractEvent(
      MatchPlayer player,
      ClickType clickType,
      @Nullable Block clickedBlock,
      @Nullable Entity clickedEntity,
      @Nullable ItemStack clickedItem) {
    super(player);
    this.clickType = checkNotNull(clickType);
    this.clickedBlock = clickedBlock;
    this.clickedEntity = clickedEntity;
    this.clickedItem = clickedItem;
  }

  public ClickType getClickType() {
    return clickType;
  }

  public @Nullable Block getClickedBlock() {
    return clickedBlock;
  }

  public @Nullable BlockState getClickedBlockState() {
    return getClickedBlock() == null ? null : getClickedBlock().getState();
  }

  public @Nullable Entity getClickedEntity() {
    return clickedEntity;
  }

  public @Nullable ItemStack getClickedItem() {
    return clickedItem;
  }

  public @Nullable MatchPlayer getClickedPlayer() {
    return getMatch().getPlayer(getClickedEntity());
  }

  public @Nullable MatchPlayer getClickedParticipant() {
    return getMatch().getParticipant(getClickedEntity());
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }

  private static HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
