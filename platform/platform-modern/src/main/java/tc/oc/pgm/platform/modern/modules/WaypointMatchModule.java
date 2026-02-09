package tc.oc.pgm.platform.modern.modules;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NonNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.wool.WoolMatchModule;

@ListenerScope(MatchScope.LOADED)
public class WaypointMatchModule implements MatchModule, Listener {

  private final Match match;
  private final ServerWaypointManager waypointManager;

  public WaypointMatchModule(Match match) {
    this.match = match;
    this.waypointManager = ((CraftWorld) match.getWorld()).getHandle().getWaypointManager();
  }

  @EventHandler
  public void afterLoad(MatchLoadEvent event) {
    var opt = match.moduleOptional(WoolMatchModule.class).orElse(null);
    if (opt == null) return;

    opt.getWools().values().forEach(wool -> {
      var loc = wool.getDefinition().getLocation();
      if (loc == null) return;

      waypointManager.trackWaypoint(new StaticWaypointTransmitter(
          new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), wool.getColor()));
    });
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinMatchEvent event) {
    var attr = event.getPlayer().getBukkit().getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
    if (attr != null) attr.setBaseValue(0);
  }

  static class StaticWaypointTransmitter implements WaypointTransmitter {
    private final UUID uuid = UUID.randomUUID();
    private final BlockPos position;
    private final Icon icon;

    public StaticWaypointTransmitter(@NonNull BlockPos position, Color color) {
      this.position = position;
      this.icon = new Icon();
      this.icon.color = Optional.of(color.asRGB());
    }

    @Override
    public boolean isTransmittingWaypoint() {
      return true;
    }

    @Override
    public @NonNull Optional<Connection> makeWaypointConnectionWith(@NonNull ServerPlayer player) {
      return Optional.of(new StaticBlockConnection(player));
    }

    @Override
    public @NonNull Icon waypointIcon() {
      return icon;
    }

    /** NMS's impl of BlockConnection requires a LivingEntity, so we made our own */
    public class StaticBlockConnection implements BlockConnection {
      private final ServerPlayer receiver;
      private BlockPos lastPosition;

      public StaticBlockConnection(ServerPlayer receiver) {
        this.receiver = receiver;
        this.lastPosition = position;
      }

      public void connect() {
        this.receiver.connection.send(
            ClientboundTrackedWaypointPacket.addWaypointPosition(uuid, icon, this.lastPosition));
      }

      public void disconnect() {
        this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(uuid));
      }

      public void update() {
        BlockPos blockPos = position;
        if (blockPos.distManhattan(this.lastPosition) > 0) {
          this.receiver.connection.send(
              ClientboundTrackedWaypointPacket.updateWaypointPosition(uuid, icon, blockPos));
          this.lastPosition = blockPos;
        }
      }

      public int distanceManhattan() {
        return this.lastPosition.distManhattan(position);
      }
    }
  }
}
