package tc.oc.pgm.platform.modern.modules.waypoints;

import io.papermc.paper.math.Position;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.bukkit.Color;
import org.bukkit.util.Vector;
import tc.oc.pgm.core.Core;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.wool.MonumentWool;

@SuppressWarnings("UnstableApiUsage")
public interface Waypoints {

  static WaypointTransmitter immutable(Vector loc, Color color) {
    return new ImmutableWaypointTransmitter(
        new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), color.asRGB());
  }

  static WaypointTransmitter immutable(Position loc, TextColor color) {
    return new ImmutableWaypointTransmitter(toBlockPos(loc), color.value());
  }

  static WaypointTransmitter from(Goal<?> goal) {
    if (!goal.hasShowOption(ShowOption.SHOW_WAYPOINT)) return null;
    return switch (goal) {
      case MonumentWool w -> new WoolWaypointTransmitter(w);
      case Flag f -> new FlagWaypointTransmitter(f);
      case Core c ->
        new GoalWaypoint.Immovable<>(
            c,
            toBlockPos(c.getLavaRegion().getBounds().getCenterPoint()),
            c.getOwner().getFullColor().asRGB());
      case Destroyable d ->
        new GoalWaypoint.Immovable<>(
            d,
            toBlockPos(d.getBlockRegion().getBounds().getCenterPoint()),
            d.getOwner().getFullColor().asRGB());
      default -> null;
    };
  }

  static BlockPos toBlockPos(Vector loc) {
    if (loc == null) return null;
    return new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  private static BlockPos toBlockPos(Position loc) {
    return new BlockPos(loc.blockX(), loc.blockY(), loc.blockZ());
  }
}
