package tc.oc.pgm.platform.sportpaper.packets;

import static net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo.EnumPlayerInfoAction.*;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.platform.bukkit.MinecraftComponentSerializer;
import net.kyori.adventure.text.Component;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_8_R3.WorldSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.scoreboard.CraftTeam;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.NameTagVisibility;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.platform.sportpaper.utils.Skins;
import tc.oc.pgm.util.nms.EnumPlayerInfoAction;
import tc.oc.pgm.util.nms.packets.Packet;
import tc.oc.pgm.util.nms.packets.TabPackets;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.skin.Skin;

@Supports(SPORTPAPER)
public class SpTabPackets implements TabPackets, PacketSender {

  @Override
  public PlayerInfo createPlayerInfoPacket(EnumPlayerInfoAction action) {
    return new SpPlayerInfo(new PacketPlayOutPlayerInfo(toNmsAction(action)));
  }

  @Override
  public Packet spawnPlayerPacket(int entityId, UUID uuid, Location location, Player player) {
    DataWatcher dataWatcher = copyDataWatcher(player);
    return new SpPacket<>(new PacketPlayOutNamedEntitySpawn(
        entityId,
        uuid,
        location.getX(),
        location.getY(),
        location.getZ(),
        (byte) location.getYaw(),
        (byte) location.getPitch(),
        CraftItemStack.asNMSCopy(null),
        dataWatcher));
  }

  @Override
  public Packet teamPacket(
      TeamPacketOperation operation,
      String name,
      String displayName,
      String prefix,
      String suffix,
      boolean friendlyFire,
      boolean seeFriendlyInvisibles,
      NameTagVisibility nameTagVisibility,
      Collection<String> players) {
    PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();
    packet.a = name;
    packet.b = displayName;
    packet.c = prefix;
    packet.d = suffix;
    packet.e = nameTagVisibility == null ? null : CraftTeam.bukkitToNotch(nameTagVisibility).e;
    // packet.f = color
    packet.g = players == null ? Lists.newArrayList() : players;
    packet.h = operation.ordinal();
    if (friendlyFire) {
      packet.i |= 1;
    }
    if (seeFriendlyInvisibles) {
      packet.i |= 2;
    }

    return new SpPacket<>(packet);
  }

  @Override
  public void removeAndAddAllTabPlayers(Player viewer) {
    List<EntityPlayer> players = new ArrayList<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (viewer.canSee(player) || player == viewer)
        players.add(((CraftPlayer) player).getHandle());
    }

    send(new PacketPlayOutPlayerInfo(REMOVE_PLAYER, players), viewer);
    send(new PacketPlayOutPlayerInfo(ADD_PLAYER, players), viewer);
  }

  private static PacketPlayOutPlayerInfo.EnumPlayerInfoAction toNmsAction(
      EnumPlayerInfoAction action) {
    return switch (action) {
      case ADD_PLAYER -> ADD_PLAYER;
      case UPDATE_LATENCY -> UPDATE_LATENCY;
      case UPDATE_DISPLAY_NAME -> UPDATE_DISPLAY_NAME;
      case REMOVE_PLAYER -> REMOVE_PLAYER;
    };
  }

  private static DataWatcher copyDataWatcher(Player player) {
    DataWatcher original = ((CraftPlayer) player).getHandle().getDataWatcher();
    List<DataWatcher.WatchableObject> values = original.c();
    DataWatcher copy = new DataWatcher(null);
    for (DataWatcher.WatchableObject value : values) {
      copy.a(value.a(), value.b());
    }
    return copy;
  }

  @SuppressWarnings("UnstableApiUsage")
  class SpPlayerInfo extends SpPacket<PacketPlayOutPlayerInfo> implements PlayerInfo {
    private static final MinecraftComponentSerializer SERIALIZER =
        MinecraftComponentSerializer.get();

    public SpPlayerInfo(PacketPlayOutPlayerInfo packet) {
      super(packet);
    }

    @Override
    public void addPlayerInfo(
        UUID uuid, String name, int ping, @Nullable Skin skin, @Nullable Component displayName) {
      GameProfile profile = new GameProfile(uuid, name);
      if (skin != null) Skins.toProfile(profile, skin);

      var nmsComponent =
          displayName == null ? null : (IChatBaseComponent) SERIALIZER.serialize(displayName);

      packet.b.add(packet
      .new PlayerInfoData(profile, ping, WorldSettings.EnumGamemode.SURVIVAL, nmsComponent));
    }

    @Override
    public boolean isNotEmpty() {
      return !packet.b.isEmpty();
    }
  }
}
