package tc.oc.pgm.platform.modern.modules.waypoints;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.wool.MonumentWool;

public class WoolWaypointTransmitter extends GoalWaypoint<MonumentWool> {
  private final @Nullable BlockPos woolLocation;
  private final BlockPos monumentLocation;

  WoolWaypointTransmitter(MonumentWool wool) {
    super(wool, wool.getColor().asRGB());

    var def = wool.getDefinition();
    this.woolLocation = Waypoints.toBlockPos(def.getLocation());
    this.monumentLocation =
        Waypoints.toBlockPos(def.getPlacementRegion().getBounds().getCenterPoint());
  }

  @Override
  public boolean shouldSee(ServerPlayer player) {
    return isTransmittingWaypoint() && (woolLocation != null || hasWool(player));
  }

  @Override
  public BlockPos position(ServerPlayer receiver) {
    return woolLocation == null || hasWool(receiver) ? monumentLocation : woolLocation;
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
