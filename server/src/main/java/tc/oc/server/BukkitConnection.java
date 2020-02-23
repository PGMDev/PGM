package tc.oc.server;

import static tc.oc.util.reflect.ReflectionUtils.readField;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import net.minecraft.server.v1_8_R3.EnumProtocolDirection;
import net.minecraft.server.v1_8_R3.HandshakeListener;
import net.minecraft.server.v1_8_R3.LazyInitVar;
import net.minecraft.server.v1_8_R3.LegacyPingHandler;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.NetworkManager;
import net.minecraft.server.v1_8_R3.PacketDecoder;
import net.minecraft.server.v1_8_R3.PacketEncoder;
import net.minecraft.server.v1_8_R3.PacketPrepender;
import net.minecraft.server.v1_8_R3.PacketSplitter;
import net.minecraft.server.v1_8_R3.ServerConnection;

public class BukkitConnection extends ServerConnection {

  protected final MinecraftServer server;

  public BukkitConnection(MinecraftServer server) {
    super(server);
    this.server = server;
  }

  @Override
  public void a(InetAddress address, int port) {
    listen(address, port, server.getPropertyManager().getBoolean("proxy-protocol", false));
  }

  protected void setupPipeline(ChannelPipeline pipeline) {
    final NetworkManager manager = new NetworkManager(EnumProtocolDirection.SERVERBOUND);
    manager.a(new HandshakeListener(server, manager)); // setPacketListener
    readField(ServerConnection.class, this, List.class, "pending")
        .add(manager); // List<NetworkManager>

    pipeline
        .addLast("timeout", new ReadTimeoutHandler(30))
        .addLast("legacy_query", new LegacyPingHandler(BukkitConnection.this))
        .addLast("splitter", new PacketSplitter())
        .addLast("decoder", new PacketDecoder(EnumProtocolDirection.SERVERBOUND))
        .addLast("prepender", new PacketPrepender())
        .addLast("encoder", new PacketEncoder(EnumProtocolDirection.CLIENTBOUND))
        .addLast("packet_handler", manager);
  }

  protected void listen(InetAddress address, int port, boolean proxyProtocol) {
    final Class<? extends ServerSocketChannel> type;
    final LazyInitVar<? extends MultithreadEventLoopGroup> group;
    if (Epoll.isAvailable() && server.ai() /* useNativeTransport */) {
      type = EpollServerSocketChannel.class;
      group = b;
    } else {
      type = NioServerSocketChannel.class;
      group = a;
    }

    final ChannelFuture channel =
        new ServerBootstrap()
            .channel(type)
            .childHandler(
                new ChannelInitializer() {
                  protected void initChannel(Channel channel) {
                    try {
                      channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                    } catch (ChannelException var3) {
                      // No-op
                    }

                    setupPipeline(channel.pipeline());
                    if (proxyProtocol) {
                      channel
                          .pipeline()
                          .addFirst("haproxy_inspector", new HAProxySourceAddressDecoder());
                      channel.pipeline().addFirst("haproxy_decoder", new HAProxyMessageDecoder());
                    }
                  }
                })
            .group(group.c() /* lazyGet */)
            .localAddress(address, port)
            .bind()
            .syncUninterruptibly();

    readField(ServerConnection.class, BukkitConnection.this, List.class, "g")
        .add(channel); // List<ChannelFuture>
  }

  protected static class HAProxySourceAddressDecoder extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
      if (message instanceof HAProxyMessage) {
        final HAProxyMessage header = (HAProxyMessage) message;
        context.pipeline().get(NetworkManager.class).l =
            new InetSocketAddress(header.sourceAddress(), header.sourcePort());
        return;
      } else if (context.pipeline().get(HAProxyMessageDecoder.class) == null) {
        context.pipeline().remove(this);
      }
      super.channelRead(context, message);
    }
  }
}
