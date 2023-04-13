package tc.oc.pgm.api.filter.query;

import org.bukkit.block.BlockState;

public interface BlockQuery extends MatchQuery, LocationQuery, MaterialQuery, InventoryQuery {
  BlockState getBlock();
}
