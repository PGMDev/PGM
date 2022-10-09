package tc.oc.pgm.filters;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;

/** Utility methods useful for implementing filters */
public class Filters {
  private Filters() {}

  /**
   * Query Argument Coercion Helpers
   *
   * <p>These methods are meant to be used on query arguments to coerce them to particular types. If
   * the helper returns null, the object is not coercible to that type and the filter should
   * abstain.
   */

  /** If the query argument is a block, return its state */
  public static @Nullable BlockState toBlockState(Object obj) {
    if (obj instanceof BlockState) {
      return (BlockState) obj;
    } else if (obj instanceof Block) {
      return ((Block) obj).getState();
    } else {
      return null;
    }
  }

  /** If the query argument is a block, return its position */
  public static @Nullable BlockVector toBlockVector(Object obj) {
    if (obj instanceof Block) {
      return ((Block) obj).getLocation().toVector().toBlockVector();
    } else if (obj instanceof BlockState) {
      return ((BlockState) obj).getLocation().toVector().toBlockVector();
    } else {
      return null;
    }
  }
}
