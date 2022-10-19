package tc.oc.pgm.events;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;

/**
 * A version of BlockTransformEvent where the block transformation can be attributed to a player.
 */
public class PlayerBlockTransformEvent extends BlockTransformEvent {
  protected final MatchPlayerState player;

  public PlayerBlockTransformEvent(
      Event cause, BlockState oldState, BlockState newState, MatchPlayerState player) {
    super(cause, oldState, newState);
    this.player = assertNotNull(player);
  }

  public PlayerBlockTransformEvent(
      Event cause, Block oldBlock, Material newMaterial, MatchPlayerState player) {
    super(cause, oldBlock, newMaterial);
    this.player = assertNotNull(player);
  }

  public MatchPlayerState getPlayerState() {
    return player;
  }

  public @Nullable MatchPlayer getPlayer() {
    return player.getPlayer().orElse(null);
  }

  @Override
  public @Nullable Player getActor() {
    MatchPlayer matchPlayer = getPlayer();
    return matchPlayer == null ? null : matchPlayer.getBukkit();
  }

  public static @Nullable MatchPlayerState getPlayerState(BlockTransformEvent event) {
    return event instanceof PlayerBlockTransformEvent
        ? ((PlayerBlockTransformEvent) event).getPlayerState()
        : null;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
        + "{pos="
        + this.getOldState().getLocation().toVector()
        + " oldState="
        + this.getOldState().getData()
        + " newState="
        + this.getNewState().getData()
        + " drops="
        + this.getDrops()
        + " cancelled="
        + this.isCancelled()
        + " player="
        + this.getPlayerState()
        + " cause="
        + (this.getCause() == null ? "null" : this.getCause().getEventName())
        + "}";
  }
}
