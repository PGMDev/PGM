package tc.oc.pgm.platform.modern.modules.waypoint.types;

import net.minecraft.core.BlockPos;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.bukkit.Color;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.controlpoint.ControlPoint;
import tc.oc.pgm.core.Core;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.payload.Payload;
import tc.oc.pgm.regions.EmptyRegion;
import tc.oc.pgm.wool.MonumentWool;

public interface Waypoints {

  static WaypointTransmitter immutable(Vector loc, Color color) {
    return new ImmutableWaypointTransmitter(
        new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), color.asRGB());
  }

  static WaypointTransmitter immutable(BlockPos pos, int color) {
    if (pos == null) return null;
    return new ImmutableWaypointTransmitter(pos, color);
  }

  static WaypointTransmitter from(Goal<?> goal) {
    if (!goal.hasShowOption(ShowOption.SHOW_WAYPOINT)) return null;
    return switch (goal) {
      case MonumentWool w -> new WoolWaypointTransmitter(w);
      case Flag f -> new FlagWaypointTransmitter(f);
      case Payload p -> new PayloadWaypoint(p);
      case ControlPoint c -> new ControlPointWaypoint(c);
      case Core c ->
        GoalWaypoint.simple(
            c,
            toBlockPos(c.getMatch(), c.getLavaRegion(), c.getDefinition().getRegion()),
            c.getOwner().getFullColor().asRGB());
      case Destroyable d ->
        GoalWaypoint.simple(
            d,
            toBlockPos(d.getMatch(), d.getBlockRegion(), d.getDefinition().getRegion()),
            d.getOwner().getFullColor().asRGB());
      default -> null;
    };
  }

  static BlockPos toBlockPos(Vector loc) {
    if (loc == null) return null;
    return new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  static <T extends Goal<?>> BlockPos toBlockPos(Match match, Region region) {
    return toBlockPos(match, region, EmptyRegion.INSTANCE);
  }

  static <T extends Goal<?>> BlockPos toBlockPos(Match match, Region region, Region containing) {
    var staticReg = region.getStatic(match);
    var bounds = staticReg.getBounds();
    var center = bounds.getCenterPoint();
    var size = bounds.getSize();
    // Region must either be small enough, or contain its own center
    // If not, we assume some union of far-apart things, and showing the center makes no sense
    boolean isSmall = size.getX() < 15 && size.getY() < 30 && size.getZ() < 15;
    if (isSmall || staticReg.contains(center) || containing.getStatic(match).contains(center))
      return toBlockPos(center);
    return null;
  }
}
