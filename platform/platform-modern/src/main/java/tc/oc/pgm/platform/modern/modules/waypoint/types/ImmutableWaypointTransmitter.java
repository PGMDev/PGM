package tc.oc.pgm.platform.modern.modules.waypoint.types;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

class ImmutableWaypointTransmitter extends AbstractWaypointTransmitter {
  private final BlockPos blockPos;

  ImmutableWaypointTransmitter(BlockPos pos, Integer color) {
    super(null);
    this.blockPos = pos;
    this.waypointIcon.color = Optional.ofNullable(color);
  }

  @Override
  public BlockPos position(ServerPlayer receiver) {
    return blockPos;
  }
}
