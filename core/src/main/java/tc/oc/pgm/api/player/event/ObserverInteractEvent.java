package tc.oc.pgm.api.player.event;

import static tc.oc.pgm.util.Assert.assertNotNull;

import lombok.Getter;
import lombok.Setter;
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

  /** The type of {@link ClickType} interaction. */
  @Getter
  private final ClickType clickType;

  /** The optional {@link Block} that was clicked. */
  @Getter
  private final @Nullable Block clickedBlock;

  /** The optional {@link Entity} that was clicked. */
  @Getter
  private final @Nullable Entity clickedEntity;

  /** The optional {@link ItemStack} that was clicked. */
  @Getter
  private final @Nullable ItemStack clickedItem;

  @Getter
  @Setter
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
   * Get the optional {@link BlockState} that was clicked.
   *
   * @return The {@link BlockState}, or {@code null} if no {@link BlockState} was clicked.
   */
  public @Nullable BlockState getClickedBlockState() {
    return getClickedBlock() == null ? null : getClickedBlock().getState();
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

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
