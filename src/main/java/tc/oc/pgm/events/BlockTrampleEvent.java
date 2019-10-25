package tc.oc.pgm.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/** Player trampled a block (walked on it). Mob trampling is not currently implemented. */
public class BlockTrampleEvent extends AdventureModeInteractEvent {
  public BlockTrampleEvent(Player player, Block block) {
    super(player, block);
  }
}
