package tc.oc.pgm.wool;

import org.bukkit.block.BlockState;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.api.player.ParticipantState;

public class PlayerWoolPlaceEvent extends MatchEvent {
  private static final HandlerList handlers = new HandlerList();

  private final ParticipantState player;
  private final MonumentWool wool;
  private final BlockState block;

  public PlayerWoolPlaceEvent(ParticipantState player, MonumentWool wool, BlockState block) {
    super(player.getMatch());
    this.player = player;
    this.wool = wool;
    this.block = block;
  }

  public ParticipantState getPlayer() {
    return this.player;
  }

  public MonumentWool getWool() {
    return this.wool;
  }

  public BlockState getBlock() {
    return this.block;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
