package tc.oc.pgm.events;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;

public class ParticipantBlockTransformEvent extends PlayerBlockTransformEvent {

  public ParticipantBlockTransformEvent(
      Event cause, BlockState oldState, BlockState newState, ParticipantState player) {
    super(cause, oldState, newState, player);
  }

  public ParticipantBlockTransformEvent(
      Event cause, Block oldBlock, Material newMaterial, ParticipantState player) {
    super(cause, oldBlock, newMaterial, player);
  }

  @Override
  public ParticipantState getPlayerState() {
    return (ParticipantState) super.getPlayerState();
  }

  public static @Nullable ParticipantState getPlayerState(BlockTransformEvent event) {
    return event instanceof ParticipantBlockTransformEvent
        ? ((ParticipantBlockTransformEvent) event).getPlayerState()
        : null;
  }

  public static @Nullable MatchPlayer getParticipant(BlockTransformEvent event) {
    ParticipantState state = getPlayerState(event);
    return state == null ? null : state.getPlayer().orElse(null);
  }
}
