package tc.oc.pgm.renewable;

import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFormEvent;

@Getter
public class BlockRenewEvent extends BlockFormEvent {

  private final Renewable renewable;

  public BlockRenewEvent(Block block, BlockState newState, Renewable renewable) {
    super(block, newState);
    this.renewable = renewable;
  }
}
