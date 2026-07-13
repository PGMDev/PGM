package tc.oc.pgm.platform.modern.modules.waypoint.types;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;
import tc.oc.pgm.platform.modern.modules.waypoint.WaypointDefinition;

public class DefinedWaypointTransmitter extends AbstractWaypointTransmitter {

  private final Mannequin mannequin;

  public DefinedWaypointTransmitter(WaypointDefinition definition, Mannequin mannequin) {
    super(null);
    this.mannequin = mannequin;
    waypointIcon.style = definition.getStyle();
    waypointIcon.color = Optional.of(definition.getColor().asRGB());
  }

  @Override
  public @Nullable BlockPos position(ServerPlayer player) {
    return Waypoints.toBlockPos(mannequin.getLocation().toVector());
  }
}
