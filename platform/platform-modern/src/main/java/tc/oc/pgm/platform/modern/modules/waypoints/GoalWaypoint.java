package tc.oc.pgm.platform.modern.modules.waypoints;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.goals.Goal;

public class GoalWaypoint<T extends Goal<?>> extends AbstractWaypointTransmitter {
  protected final T goal;
  protected BlockPos position;

  protected GoalWaypoint(T goal, int color) {
    this(goal, color, null);
  }

  protected GoalWaypoint(T goal, int color, @Nullable UUID uuid) {
    super(uuid);
    this.goal = goal;
    this.waypointIcon.color = Optional.of(color);
  }

  public static <T extends Goal<?>> GoalWaypoint<T> simple(T goal, BlockPos pos, int color) {
    var waypoint = new GoalWaypoint<>(goal, color);
    waypoint.position = pos;
    return waypoint;
  }

  @Override
  public boolean isTransmittingWaypoint() {
    return !goal.isCompleted();
  }

  @Override
  public @Nullable BlockPos position(ServerPlayer player) {
    return position;
  }

  protected void setPosition(@Nullable Location loc) {
    setPosition(loc == null ? null : loc.toVector());
  }

  protected void setPosition(@Nullable Vector pos) {
    this.position = pos == null
        ? null
        : this.position == null
                || position.getX() != pos.getBlockX()
                || position.getY() != pos.getBlockY()
                || position.getZ() != pos.getBlockZ()
            ? Waypoints.toBlockPos(pos)
            : this.position;
  }
}
