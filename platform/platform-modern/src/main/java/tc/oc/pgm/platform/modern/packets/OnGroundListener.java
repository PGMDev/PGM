package tc.oc.pgm.platform.modern.packets;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.papermc.paper.event.player.PlayerFailMoveEvent;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.HandlerNames;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.event.player.PlayerOnGroundEvent;

@NullMarked
public class OnGroundListener implements Listener {
  private static final String HANDLER_NAME = "pgm_on_ground";

  private final ChannelHandler handler = new MovePacketWrapper();
  private final Map<ServerPlayer, Boolean> reported = new WeakHashMap<>();
  private @Nullable ServerPlayer handling;
  private boolean rejected;

  public OnGroundListener() {
    Bukkit.getOnlinePlayers().forEach(this::inject);
  }

  public void unregister() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      Channel channel = channel(player);
      channel.eventLoop().execute(() -> {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(HANDLER_NAME) != null) pipeline.remove(HANDLER_NAME);
      });
    }
  }

  private static Channel channel(Player player) {
    return ((CraftPlayer) player).getHandle().connection.connection.channel;
  }

  private void inject(Player player) {
    ChannelPipeline pipeline = channel(player).pipeline();
    if (pipeline.get(HANDLER_NAME) == null && pipeline.get(HandlerNames.PACKET_HANDLER) != null) {
      pipeline.addBefore(HandlerNames.PACKET_HANDLER, HANDLER_NAME, handler);
    }
  }

  private void handle(ServerGamePacketListener listener, Runnable vanilla) {
    if (!(listener instanceof ServerGamePacketListenerImpl impl) || !Bukkit.isPrimaryThread()) {
      vanilla.run();
      return;
    }

    ServerPlayer player = impl.player;
    boolean wasOnGround = reported.computeIfAbsent(player, ServerPlayer::onGround);
    handling = player;
    rejected = false;
    try {
      vanilla.run();
    } finally {
      handling = null;
    }

    if (rejected) return;
    boolean onGround = player.onGround();
    if (wasOnGround == onGround) return;
    reported.put(player, onGround);
    if (player.isPassenger() || player.isSleeping()) return;
    new PlayerOnGroundEvent(player.getBukkitEntity(), onGround).callEvent();
  }

  private boolean isHandling(Player player) {
    return handling != null && handling.getBukkitEntity() == player;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    inject(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMove(PlayerMoveEvent event) {
    if (event instanceof PlayerTeleportEvent) return;
    if (event.isCancelled() && isHandling(event.getPlayer())) rejected = true;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerFailMove(PlayerFailMoveEvent event) {
    if (isHandling(event.getPlayer())) rejected = !event.isAllowed();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerTeleport(PlayerTeleportEvent event) {
    if (isHandling(event.getPlayer())) rejected = true;
  }

  @ChannelHandler.Sharable
  private class MovePacketWrapper extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      ctx.fireChannelRead(msg instanceof ServerboundMovePlayerPacket packet ? wrap(packet) : msg);
    }

    private ServerboundMovePlayerPacket wrap(ServerboundMovePlayerPacket p) {
      boolean onGround = p.isOnGround();
      boolean collision = p.horizontalCollision();
      return switch (p) {
        case ServerboundMovePlayerPacket.PosRot ignored ->
          new PosRot(
              p.getX(0), p.getY(0), p.getZ(0), p.getYRot(0), p.getXRot(0), onGround, collision);
        case ServerboundMovePlayerPacket.Pos ignored ->
          new Pos(p.getX(0), p.getY(0), p.getZ(0), onGround, collision);
        case ServerboundMovePlayerPacket.Rot ignored ->
          new Rot(p.getYRot(0), p.getXRot(0), onGround, collision);
        case ServerboundMovePlayerPacket.StatusOnly ignored -> new StatusOnly(onGround, collision);
        default -> p;
      };
    }
  }

  private class Pos extends ServerboundMovePlayerPacket.Pos {
    Pos(double x, double y, double z, boolean onGround, boolean horizontalCollision) {
      super(x, y, z, onGround, horizontalCollision);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
      OnGroundListener.this.handle(listener, () -> super.handle(listener));
    }
  }

  private class PosRot extends ServerboundMovePlayerPacket.PosRot {
    PosRot(
        double x,
        double y,
        double z,
        float yRot,
        float xRot,
        boolean onGround,
        boolean horizontalCollision) {
      super(x, y, z, yRot, xRot, onGround, horizontalCollision);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
      OnGroundListener.this.handle(listener, () -> super.handle(listener));
    }
  }

  private class Rot extends ServerboundMovePlayerPacket.Rot {
    Rot(float yRot, float xRot, boolean onGround, boolean horizontalCollision) {
      super(yRot, xRot, onGround, horizontalCollision);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
      OnGroundListener.this.handle(listener, () -> super.handle(listener));
    }
  }

  private class StatusOnly extends ServerboundMovePlayerPacket.StatusOnly {
    StatusOnly(boolean onGround, boolean horizontalCollision) {
      super(onGround, horizontalCollision);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
      OnGroundListener.this.handle(listener, () -> super.handle(listener));
    }
  }
}
