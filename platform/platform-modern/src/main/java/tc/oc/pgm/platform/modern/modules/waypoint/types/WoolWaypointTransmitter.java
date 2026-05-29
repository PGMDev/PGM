package tc.oc.pgm.platform.modern.modules.waypoint.types;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import tc.oc.pgm.wool.MonumentWool;

public class WoolWaypointTransmitter extends GoalWaypoint<MonumentWool> {
  private final BlockPos monumentLocation;

  WoolWaypointTransmitter(MonumentWool wool) {
    super(wool, wool.getColor().asRGB());

    var def = wool.getDefinition();
    var loc = def.getLocation();
    var len = loc.lengthSquared();
    setPosition(len == 0 || Double.isInfinite(len) ? null : loc);
    this.monumentLocation = Waypoints.toBlockPos(wool.getMatch(), def.getPlacementRegion());
  }

  @Override
  public boolean shouldSee(ServerPlayer player) {
    return isTransmittingWaypoint() && (position != null || hasWool(player));
  }

  @Override
  public BlockPos position(ServerPlayer receiver) {
    return position == null || hasWool(receiver) ? monumentLocation : position;
  }

  private boolean hasWool(ServerPlayer player) {
    if (goal.isCompleted()) return false;
    var mp = goal.getMatch().getPlayer(player.getBukkitEntity());
    return mp != null
        && mp.isParticipating()
        && goal.isTouched()
        && goal.hasTouched(mp.getParticipantState())
        && goal.getDefinition().isHolding(mp);
  }
}
