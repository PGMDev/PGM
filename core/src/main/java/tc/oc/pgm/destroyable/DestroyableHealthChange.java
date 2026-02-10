package tc.oc.pgm.destroyable;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;

/**
 * Provides additional information about why a {@link Destroyable} was damaged.
 *
 * @param oldState State the destroyed block was in before broken
 * @param newState Gets the state of the block after being damaged
 * @param playerCause Player most responsible for the damage, or null if none exists
 */
public record DestroyableHealthChange(
    @NonNull BlockState oldState,
    @NonNull BlockState newState,
    @Nullable ParticipantState playerCause,
    int healthChange) {

  public DestroyableHealthChange {
    assertNotNull(oldState, "old block state");
  }

  /**
   * Gets the block that was damaged.
   *
   * @return Damaged block
   */
  public @NonNull Block getBlock() {
    return this.oldState.getBlock();
  }

  @Deprecated
  public @NonNull BlockState getOldState() {
    return oldState();
  }

  @Deprecated
  public @NonNull BlockState getNewState() {
    return newState();
  }

  @Deprecated
  public @Nullable ParticipantState getPlayerCause() {
    return playerCause();
  }

  @Deprecated
  public int getHealthChange() {
    return healthChange();
  }
}
