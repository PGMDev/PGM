package tc.oc.pgm.filters;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import tc.oc.pgm.filters.query.IBlockQuery;
import tc.oc.pgm.listeners.WorldProblemListener;

/** Matches blocks that have only air/void below them */
public class VoidFilter extends TypedFilter<IBlockQuery> {

  @Override
  public Class<? extends IBlockQuery> getQueryType() {
    return IBlockQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(IBlockQuery query) {
    BlockState block = query.getBlock();
    return QueryResponse.fromBoolean(
        block.getY() == 0
            || (!WorldProblemListener.wasBlock36(block.getWorld(), block.getX(), 0, block.getZ())
                && block.getWorld().getBlockAt(block.getX(), 0, block.getZ()).getType()
                    == Material.AIR));
  }

  @Override
  public String toString() {
    return "VoidFilter{}";
  }
}
