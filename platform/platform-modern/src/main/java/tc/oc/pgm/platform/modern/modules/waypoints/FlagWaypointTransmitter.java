package tc.oc.pgm.platform.modern.modules.waypoints;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.state.Carried;

public class FlagWaypointTransmitter extends GoalWaypoint<Flag> {

  FlagWaypointTransmitter(Flag flag) {
    super(flag, flag.getDyeColor().getColor().asRGB());
  }

  @Override
  public boolean isTransmittingWaypoint() {
    // Carried flags will use the player was waypoint instead, so that movement is smooth
    return super.isTransmittingWaypoint() && !goal.isCurrent(Carried.class);
  }

  @Override
  public BlockPos position(ServerPlayer receiver) {
    return goal.getLocation().map(l -> Waypoints.toBlockPos(l.toVector())).orElse(null);
  }
}
