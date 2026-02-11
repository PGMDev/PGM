package tc.oc.pgm.platform.modern.modules.waypoints;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import tc.oc.pgm.goals.Goal;

public abstract class GoalWaypoint<T extends Goal<?>> extends AbstractWaypointTransmitter {
  protected final T goal;

  GoalWaypoint(T goal, int color) {
    this.goal = goal;
    this.waypointIcon.color = Optional.of(color);
  }

  @Override
  public boolean isTransmittingWaypoint() {
    return !goal.isCompleted();
  }

  static class Immovable<T extends Goal<?>> extends GoalWaypoint<T> {
    private final BlockPos pos;

    public Immovable(T goal, BlockPos pos, int color) {
      super(goal, color);
      this.pos = pos;
    }

    @Override
    public BlockPos position(ServerPlayer player) {
      return pos;
    }
  }
}
