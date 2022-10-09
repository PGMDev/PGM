package tc.oc.pgm.fallingblocks;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.query.BlockQuery;
import tc.oc.pgm.util.material.Materials;

public class FallingBlocksRule {
  public static final int DEFAULT_DELAY = 2;

  public final Filter fall;
  public final Filter stick;
  public final int delay;

  public FallingBlocksRule(Filter fall, Filter stick, int delay) {
    this.fall = fall;
    this.stick = stick;
    this.delay = delay;
  }

  public boolean canFall(Block block) {
    return this.canFall(block.getState());
  }

  public boolean canFall(BlockState block) {
    switch (this.fall.query(new BlockQuery(block))) {
      case ALLOW:
        return true;
      case DENY:
        return false;
      default:
        return block.getType().hasGravity();
    }
  }

  public boolean canSupport(BlockState supporter) {
    return this.stick.query(new BlockQuery(supporter)).isAllowed();
  }

  public boolean canSupport(Block supporter) {
    return this.stick.query(new BlockQuery(supporter)).isAllowed();
  }

  /**
   * Test if the given block is supportive from the given direction, either because this rule makes
   * the block sticky, or because it's a solid block supporting from below.
   */
  public boolean canSupport(@Nullable Block supporter, BlockFace from) {
    return supporter != null
        && (from == BlockFace.DOWN && Materials.isSolid(supporter.getType())
            || this.canSupport(supporter));
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
        + "{fall="
        + this.fall
        + " stick="
        + this.stick
        + " delay="
        + this.delay
        + "}";
  }
}
