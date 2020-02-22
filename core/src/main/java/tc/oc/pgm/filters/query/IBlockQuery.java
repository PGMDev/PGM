package tc.oc.pgm.filters.query;

import org.bukkit.block.BlockState;

public interface IBlockQuery extends IMatchQuery, ILocationQuery, IMaterialQuery {
  BlockState getBlock();
}
