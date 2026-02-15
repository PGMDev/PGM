package tc.oc.pgm.platform.modern.modules.waypoint.types;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

abstract class AbstractWaypointTransmitter implements PGMWaypointTransmitter {
  private final UUID uuid;
  protected final Icon waypointIcon = new Icon();

  protected AbstractWaypointTransmitter(@Nullable UUID uuid) {
    this.uuid = uuid != null ? uuid : UUID.randomUUID();
  }

  @Override
  public boolean isTransmittingWaypoint() {
    return true;
  }

  @Override
  public boolean shouldSee(ServerPlayer player) {
    return isTransmittingWaypoint();
  }

  @Override
  public @NonNull Optional<Connection> makeWaypointConnectionWith(@NonNull ServerPlayer player) {
    if (shouldSee(player)) {
      var connection = new PGMBlockConnection(this, player);
      if (connection.canConnect()) return Optional.of(connection);
    }
    return Optional.empty();
  }

  @Override
  public UUID uuid() {
    return uuid;
  }

  @Override
  public @NonNull Icon waypointIcon() {
    return waypointIcon;
  }
}
