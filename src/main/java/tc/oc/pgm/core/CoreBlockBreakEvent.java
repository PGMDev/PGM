package tc.oc.pgm.core;

import org.bukkit.block.BlockState;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayerState;

public class CoreBlockBreakEvent extends CoreEvent {
  private static final HandlerList handlers = new HandlerList();

  private final MatchPlayerState player;
  private final BlockState blockBroken;

  public CoreBlockBreakEvent(Core core, MatchPlayerState player, BlockState blockBroken) {
    super(player.getMatch(), core);
    this.player = player;
    this.blockBroken = blockBroken;
  }

  public MatchPlayerState getPlayer() {
    return this.player;
  }

  public BlockState getBlockBroken() {
    return this.blockBroken;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
