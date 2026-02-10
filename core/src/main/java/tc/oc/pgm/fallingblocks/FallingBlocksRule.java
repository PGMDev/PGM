package tc.oc.pgm.fallingblocks;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.query.BlockQuery;
import tc.oc.pgm.util.material.Materials;

public record FallingBlocksRule(Filter fall, Filter stick, int delay) {
  public static final int DEFAULT_DELAY = 2;

  public boolean canFall(Block block) {
    return this.canFall(block.getState());
  }

  public boolean canFall(BlockState block) {
    return switch (this.fall.query(new BlockQuery(block))) {
      case ALLOW -> true;
      case DENY -> false;
      default -> block.getType().hasGravity();
    };
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
  public @NonNull String toString() {
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
