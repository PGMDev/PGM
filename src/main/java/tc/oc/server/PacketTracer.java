package tc.oc.server;

import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ScatteringByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EnumProtocol;
import net.minecraft.server.v1_8_R3.EnumProtocolDirection;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NetworkManager;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketDataSerializer;
import net.minecraft.server.v1_8_R3.PacketDecoder;
import net.minecraft.server.v1_8_R3.PacketEncoder;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tc.oc.util.reflect.ReflectionUtils;

/**
 * Dumps in/out packets to a logger by intercepting the low-level serialization methods. This allows
 * the fields of a packet to be displayed without any particular knowledge of the packet structure.
 *
 * <p>TODO: reader methods
 */
public class PacketTracer extends PacketDataSerializer {

  private static final Map<Class<? extends Packet>, Boolean> filter = new ConcurrentHashMap<>();
  private static boolean defaultInclude = true;

  private static final Map<
          EnumProtocol, Map<EnumProtocolDirection, BiMap<Integer, Class<? extends Packet>>>>
      packets = new HashMap<>();

  static {
    synchronized (packets) {
      for (EnumProtocol proto : EnumProtocol.values()) {
        packets.put(proto, ReflectionUtils.readField(EnumProtocol.class, proto, Map.class, "j"));
      }
    }
  }

  /**
   * Find a packet class by case-insensitive substring of the class name. If multiple packets match,
   * the shortest name wins.
   */
  public static @Nullable Class<?> findPacketType(String name) {
    int minExtra = Integer.MAX_VALUE;
    Class<? extends Packet> best = null;
    name = name.toLowerCase();
    synchronized (packets) {
      for (EnumProtocol proto : packets.keySet()) {
        for (EnumProtocolDirection dir : packets.get(proto).keySet()) {
          for (Class<? extends Packet> type : packets.get(proto).get(dir).values()) {
            if (type.getName().toLowerCase().contains(name)) {
              int extra = type.getName().length() - name.length();
              if (extra < minExtra) {
                best = type;
                minExtra = extra;
              } else if (extra == minExtra) {
                best = null;
              }
            }
          }
        }
      }
    }
    return best;
  }

  /** Start tracing packets for the given player */
  public static boolean start(Player player, final Logger logger) {
    final String client = player.getName();
    final Channel channel = getChannel(player);

    final PacketEncoder oldEncoder = channel.pipeline().get(PacketEncoder.class);
    final PacketDecoder oldDecoder = channel.pipeline().get(PacketDecoder.class);

    channel
        .eventLoop()
        .execute(
            new Runnable() {
              @Override
              public void run() {
                if (channel.isOpen()) {
                  if (oldEncoder != null) {
                    channel.pipeline().replace(oldEncoder, "encoder", new Encoder(logger, client));
                  }

                  if (oldDecoder != null) {
                    channel.pipeline().replace(oldDecoder, "decoder", new Decoder(logger, client));
                  }
                }
              }
            });

    return oldEncoder != null || oldDecoder != null;
  }

  /** Stop tracing packets for the given player */
  public static boolean stop(Player player) {
    final Channel channel = getChannel(player);

    final Encoder oldEncoder = channel.pipeline().get(Encoder.class);
    final Decoder oldDecoder = channel.pipeline().get(Decoder.class);

    channel
        .eventLoop()
        .execute(
            new Runnable() {
              @Override
              public void run() {
                if (channel.isOpen()) {
                  if (oldEncoder != null) {
                    channel
                        .pipeline()
                        .replace(
                            oldEncoder,
                            "encoder",
                            new PacketEncoder(EnumProtocolDirection.CLIENTBOUND));
                  }

                  if (oldDecoder != null) {
                    channel
                        .pipeline()
                        .replace(
                            oldDecoder,
                            "decoder",
                            new PacketDecoder(EnumProtocolDirection.SERVERBOUND));
                  }
                }
              }
            });

    return oldEncoder != null || oldDecoder != null;
  }

  /** Include or exclude the given packet class from tracing */
  public static void filter(Class<?> type, boolean include) {
    filter.put(type.asSubclass(Packet.class), include);
  }

  /** Include or exclude the given packet class from tracing */
  public static boolean filter(String packetName, boolean include) {
    Class type = findPacketType(packetName);
    if (type == null) return false;
    filter(type, include);
    return true;
  }

  /** Clear all packet inclusions and exclusions */
  public static void clearFilter() {
    filter.clear();
  }

  /** Set whether packets are included or excluded by default */
  public static void setDefaultInclude(boolean include) {
    defaultInclude = include;
  }

  /** Are packets included or excluded by default? */
  public static boolean getDefaultInclude() {
    return defaultInclude;
  }

  private static Channel getChannel(Player player) {
    return ((CraftPlayer) player).getHandle().playerConnection.a().channel;
  }

  private static class Encoder extends MessageToByteEncoder<Packet> {
    private final Logger logger;
    private final String client;

    Encoder(Logger logger, String client) {
      this.logger = logger;
      this.client = client;
    }

    @Override
    protected void encode(ChannelHandlerContext context, Packet packet, ByteBuf buffer)
        throws Exception {
      Integer id =
          context
              .channel()
              .attr(NetworkManager.c)
              .get()
              .a(EnumProtocolDirection.CLIENTBOUND, packet);
      if (id == null) {
        throw new IOException("Cannot encode unregistered packet class " + packet.getClass());
      } else {
        try {
          PacketTracer dumper = new PacketTracer(buffer, logger, client);
          dumper.b(id); // write VarInt
          packet.b(dumper); // write packet
          dumper.packet(">>>", id, packet);
        } catch (Throwable e) {
          logger.log(Level.SEVERE, "Exception writing " + packet.getClass().getSimpleName(), e);
        }
      }
    }
  }

  private static class Decoder extends ByteToMessageDecoder {
    private final Logger logger;
    private final String client;

    Decoder(Logger logger, String client) {
      this.logger = logger;
      this.client = client;
    }

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf buffer, List<Object> messages)
        throws Exception {
      if (buffer.readableBytes() > 0) {
        PacketTracer dumper = new PacketTracer(buffer, logger, client);
        int id = dumper.e(); // read VarInt
        Packet packet =
            context.channel().attr(NetworkManager.c).get().a(EnumProtocolDirection.SERVERBOUND, id);

        if (packet == null) {
          throw new IOException("Cannot decode unregistered packet ID " + id);
        } else {
          packet.a(dumper); // read packet

          if (dumper.readableBytes() > 0) {
            throw new IOException(
                dumper.readableBytes()
                    + " extra bytes after reading packet "
                    + packet.getClass().getSimpleName());
          } else {
            messages.add(packet);
            dumper.packet("<<<", id, packet);
          }
        }
      }
    }
  }

  private static final Joiner fieldJoiner = Joiner.on(' ');
  private static final Joiner listJoiner = Joiner.on(',');

  private final Logger log;
  private final String client;
  private final List<String> fields = new ArrayList<>();
  private boolean mute;

  public PacketTracer(ByteBuf buf, Logger logger, String client) {
    super(buf);
    this.log = logger;
    this.client = client;
  }

  private void field(String text) {
    if (!mute) {
      fields.add(text);
    }
  }

  private void value(String type, String value) {
    field(type + ":" + value);
  }

  private void value(String type, Object value) {
    field(type + ":" + String.valueOf(value));
  }

  private void list(String type, String... values) {
    field(type + ":[" + listJoiner.join(values) + "]");
  }

  private void packet(String dir, int id, Packet packet) {
    Boolean b = filter.get(packet.getClass());
    if (b == null ? defaultInclude : b) {
      log.info(
          client
              + " "
              + dir
              + " "
              + packet.getClass().getSimpleName()
              + "("
              + id
              + ") {"
              + fieldJoiner.join(fields)
              + "}");
    }
  }

  @Override
  public void a(ItemStack stack) {
    value("ItemStack", stack);
    try {
      mute = true;
      super.a(stack);
    } finally {
      mute = false;
    }
  }

  @Override
  public void a(IChatBaseComponent chat) throws IOException {
    value("Chat", chat);
    try {
      mute = true;
      super.a(chat);
    } finally {
      mute = false;
    }
  }

  @Override
  public void a(BlockPosition pos) {
    value("BlockPosition", pos);
    try {
      mute = true;
      super.a(pos);
    } finally {
      mute = false;
    }
  }

  @Override
  public void a(NBTTagCompound nbt) {
    value("NBT", nbt);
    try {
      mute = true;
      super.a(nbt);
    } finally {
      mute = false;
    }
  }

  @Override
  public void a(UUID uuid) {
    value("UUID", uuid);
    try {
      mute = true;
      super.a(uuid);
    } finally {
      mute = false;
    }
  }

  @Override
  public PacketDataSerializer a(String s) {
    value("String", StringEscapeUtils.escapeJava(s));
    try {
      mute = true;
      return super.a(s);
    } finally {
      mute = false;
    }
  }

  @Override
  public void b(int i) {
    value("VarInt", Integer.toString(i));
    try {
      mute = true;
      super.b(i);
    } finally {
      mute = false;
    }
  }

  @Override
  public void b(long l) {
    value("VarInt", Long.toString(l));
    try {
      mute = true;
      super.b(l);
    } finally {
      mute = false;
    }
  }

  @Override
  public void a(Enum<?> e) {
    field(e.getClass().getSimpleName() + "." + e.name());
    try {
      mute = true;
      super.a(e);
    } finally {
      mute = false;
    }
  }

  @Override
  public ByteBuf writeBoolean(boolean flag) {
    value("boolean", Boolean.toString(flag));
    return super.writeBoolean(flag);
  }

  @Override
  public ByteBuf writeByte(int i) {
    value("byte", Byte.toString((byte) i));
    return super.writeByte(i);
  }

  @Override
  public ByteBuf writeShort(int i) {
    value("short", Short.toString((short) i));
    return super.writeShort(i);
  }

  @Override
  public ByteBuf writeMedium(int i) {
    value("medium", Integer.toString(i));
    return super.writeMedium(i);
  }

  @Override
  public ByteBuf writeInt(int i) {
    value("int", Integer.toString(i));
    return super.writeInt(i);
  }

  @Override
  public ByteBuf writeLong(long i) {
    value("long", Long.toString(i));
    return super.writeLong(i);
  }

  @Override
  public ByteBuf writeChar(int i) {
    value("char", Character.toString((char) i));
    return super.writeChar(i);
  }

  @Override
  public ByteBuf writeFloat(float f) {
    value("float", Float.toString(f));
    return super.writeFloat(f);
  }

  @Override
  public ByteBuf writeDouble(double d) {
    value("double", Double.toString(d));
    return super.writeDouble(d);
  }

  @Override
  public ByteBuf writeBytes(ByteBuf bytes) {
    return writeBytes(bytes, bytes.readableBytes());
  }

  @Override
  public ByteBuf writeBytes(ByteBuf bytes, int length) {
    return writeBytes(bytes, bytes.readerIndex(), length);
  }

  @Override
  public ByteBuf writeBytes(ByteBuf bytes, int start, int length) {
    String[] raw = new String[length];
    for (int i = 0; i < bytes.readableBytes(); i++) {
      raw[i] = Byte.toString(bytes.getByte(start + i));
    }
    list("byte", raw);
    return super.writeBytes(bytes, start, length);
  }

  @Override
  public ByteBuf writeBytes(byte[] bytes) {
    return writeBytes(bytes, 0, bytes.length);
  }

  @Override
  public ByteBuf writeBytes(byte[] bytes, int start, int length) {
    String[] raw = new String[length];
    for (int i = 0; i < length; i++) {
      raw[i] = Byte.toString(bytes[start + i]);
    }
    list("byte", raw);
    return super.writeBytes(bytes, start, length);
  }

  @Override
  public ByteBuf writeBytes(ByteBuffer bytes) {
    String[] raw = new String[bytes.remaining()];
    for (int i = 0; i < bytes.remaining(); i++) {
      raw[i] = Byte.toString(bytes.get(bytes.position() + i));
    }
    list("byte", raw);
    return super.writeBytes(bytes);
  }

  @Override
  public int writeBytes(InputStream istream, int max) throws IOException {
    byte[] bytes = new byte[max];
    int length = istream.read(bytes, 0, max);
    writeBytes(bytes, 0, length);
    return length;
  }

  @Override
  public int writeBytes(ScatteringByteChannel ch, int max) throws IOException {
    ByteBuffer tmp = ByteBuffer.allocateDirect(max);
    int length = ch.read(tmp);
    writeBytes(tmp);
    return length;
  }

  @Override
  public ByteBuf writeZero(int i) {
    list("byte", "0 * " + i);
    return super.writeZero(i);
  }
}
