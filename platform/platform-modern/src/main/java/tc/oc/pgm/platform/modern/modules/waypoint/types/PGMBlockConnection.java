package tc.oc.pgm.platform.modern.modules.waypoint.types;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointTransmitter;

/** NMS's impl of BlockConnection requires a LivingEntity, so we made our own */
class PGMBlockConnection implements WaypointTransmitter.Connection {
  private final ServerPlayer receiver;
  private final PGMWaypointTransmitter transmitter;
  private final Waypoint.Icon lastIcon;
  private BlockPos lastPosition, newPosition;

  public PGMBlockConnection(PGMWaypointTransmitter transmitter, ServerPlayer receiver) {
    this.receiver = receiver;
    this.transmitter = transmitter;
    this.lastIcon = new Waypoint.Icon();
    this.lastIcon.copyFrom(transmitter.waypointIcon());
    this.lastPosition = transmitter.position(receiver);
  }

  public boolean canConnect() {
    return this.lastPosition != null;
  }

  public void connect() {
    this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(
        transmitter.uuid(), lastIcon, lastPosition));
  }

  public void disconnect() {
    this.receiver.connection.send(
        ClientboundTrackedWaypointPacket.removeWaypoint(transmitter.uuid()));
  }

  public void update() {
    // It appears the client will not update the icon/color unless waypoint is removed and re-added
    // Luckily this is pretty trivial for us to do instead of an update
    var icon = transmitter.waypointIcon();
    if (iconChanged(icon)) {
      this.lastPosition = this.newPosition;
      this.lastIcon.copyFrom(icon);
      disconnect();
      connect();
    } else if (this.newPosition.distManhattan(this.lastPosition) > 0) {
      this.lastPosition = this.newPosition;

      this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointPosition(
          transmitter.uuid(), lastIcon, lastPosition));
    }
  }

  boolean iconChanged(Waypoint.Icon newIcon) {
    return !lastIcon.style.equals(newIcon.style) || !lastIcon.color.equals(newIcon.color);
  }

  @Override
  public boolean isBroken() {
    return !transmitter.shouldSee(receiver)
        || (newPosition = transmitter.position(receiver)) == null;
  }
}
