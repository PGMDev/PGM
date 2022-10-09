package tc.oc.pgm.payload.track;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Rails;
import org.jetbrains.annotations.Nullable;

public enum RailDirection {
  SOUTH(new StraightRail(BlockFace.SOUTH), new StraightRail(BlockFace.NORTH)),
  EAST(new StraightRail(BlockFace.EAST), new StraightRail(BlockFace.WEST)),
  EAST_SLOPE(new SlopedRail(BlockFace.EAST)),
  WEST_SLOPE(new SlopedRail(BlockFace.WEST)),
  NORTH_SLOPE(new SlopedRail(BlockFace.NORTH)),
  SOUTH_SLOPE(new SlopedRail(BlockFace.SOUTH)),
  SOUTH_EAST(new CurvedRail(BlockFace.SOUTH, BlockFace.EAST)),
  SOUTH_WEST(new CurvedRail(BlockFace.SOUTH, BlockFace.WEST)),
  NORTH_WEST(new CurvedRail(BlockFace.NORTH, BlockFace.WEST)),
  NORTH_EAST(new CurvedRail(BlockFace.NORTH, BlockFace.EAST));

  private static final RailDirection[] DIRECTIONS = values();

  public final RailOffset forwards;
  public final RailOffset backwards;

  RailDirection(RailOffset forwards) {
    this(forwards, forwards.reverse());
  }

  RailDirection(RailOffset forwards, RailOffset backwards) {
    this.forwards = forwards;
    this.backwards = backwards;
  }

  public static RailOffset of(Block block, @Nullable BlockFace previous) {
    RailDirection direction = getRail(block);
    if (direction == null) return null;

    if (previous == null) return direction.forwards;
    return previous.getOppositeFace() == direction.forwards.getPrevious()
        ? direction.forwards
        : direction.backwards;
  }

  public static @Nullable RailDirection getRail(Block block) {
    MaterialData md = block.getState().getMaterialData();
    if (!(md instanceof Rails)) return null;

    byte data = md.getData();
    if (data < 0 || data >= DIRECTIONS.length)
      throw new IllegalStateException(
          "Invalid rail metadata: " + data + " @ " + block.getLocation());

    return DIRECTIONS[data];
  }
}
