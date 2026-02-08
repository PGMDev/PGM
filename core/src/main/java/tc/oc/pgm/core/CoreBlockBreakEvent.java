package tc.oc.pgm.core;

import lombok.Getter;
import org.bukkit.block.BlockState;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayerState;

@Getter
public class CoreBlockBreakEvent extends CoreEvent {
  private static final HandlerList handlers = new HandlerList();

  private final MatchPlayerState player;
  private final BlockState blockBroken;

  public CoreBlockBreakEvent(Core core, MatchPlayerState player, BlockState blockBroken) {
    super(player.getMatch(), core);
    this.player = player;
    this.blockBroken = blockBroken;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
