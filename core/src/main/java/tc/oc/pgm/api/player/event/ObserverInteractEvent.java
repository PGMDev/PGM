package tc.oc.pgm.api.player.event;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;

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
    this.clickType = assertNotNull(clickType);
    this.clickedBlock = clickedBlock;
    this.clickedEntity = clickedEntity;
    this.clickedItem = clickedItem;
  }

  /**
   * Get the type of {@link ClickType} interaction.
   *
   * @return The {@link ClickType}.
   */
  public ClickType getClickType() {
    return clickType;
  }

  /**
   * Get the optional {@link Block} that was clicked.
   *
   * @return The {@link Block}, or {@code null} if no {@link Block} was clicked.
   */
  public @Nullable Block getClickedBlock() {
    return clickedBlock;
  }

  /**
   * Get the optional {@link BlockState} that was clicked.
   *
   * @return The {@link BlockState}, or {@code null} if no {@link BlockState} was clicked.
   */
  public @Nullable BlockState getClickedBlockState() {
    return getClickedBlock() == null ? null : getClickedBlock().getState();
  }

  /**
   * Get the optional {@link Entity} that was clicked.
   *
   * @return The {@link Entity}, or {@code null} if no {@link Entity} was clicked.
   */
  public @Nullable Entity getClickedEntity() {
    return clickedEntity;
  }

  /**
   * Get the optional {@link ItemStack} that was clicked.
   *
   * @return The {@link ItemStack}, or {@code null} if no {@link ItemStack} was clicked.
   */
  public @Nullable ItemStack getClickedItem() {
    return clickedItem;
  }

  /**
   * Get the optional {@link MatchPlayer} that was clicked.
   *
   * @return The {@link MatchPlayer}, or {@code null} if no {@link MatchPlayer} was clicked.
   */
  public @Nullable MatchPlayer getClickedPlayer() {
    return getMatch().getPlayer(getClickedEntity());
  }

  /**
   * Get the optional {@link MatchPlayer} participant that was clicked.
   *
   * @return The {@link MatchPlayer}, or {@code null} if no {@link MatchPlayer} was clicked.
   */
  public @Nullable MatchPlayer getClickedParticipant() {
    return getMatch().getParticipant(getClickedEntity());
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    cancelled = cancel;
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
