package tc.oc.pgm.util.nms.packets;

import static tc.oc.pgm.util.nms.packets.TabPackets.TeamPacketOperation.*;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.NameTagVisibility;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.nms.EnumPlayerInfoAction;
import tc.oc.pgm.util.skin.Skin;

public interface TabPackets {
  PlayerInfo createPlayerInfoPacket(EnumPlayerInfoAction action);

  Packet spawnPlayerPacket(int entityId, UUID uuid, Location location, Player player);

  default Packet teamCreatePacket(
      String name,
      String displayName,
      String prefix,
      String suffix,
      boolean friendlyFire,
      boolean seeFriendlyInvisibles,
      Collection<String> players) {
    return teamPacket(
        CREATE,
        name,
        displayName,
        prefix,
        suffix,
        friendlyFire,
        seeFriendlyInvisibles,
        NameTagVisibility.ALWAYS,
        players);
  }

  default Packet teamRemovePacket(String name) {
    return teamPacket(REMOVE, name, null, null, null, false, false, null, null);
  }

  default Packet teamUpdatePacket(
      String name,
      String displayName,
      String prefix,
      String suffix,
      boolean friendlyFire,
      boolean seeInvis) {
    return teamPacket(
        UPDATE,
        name,
        displayName,
        prefix,
        suffix,
        friendlyFire,
        seeInvis,
        NameTagVisibility.ALWAYS,
        Lists.newArrayList());
  }

  default Packet teamJoinPacket(String name, Collection<String> players) {
    return teamPacket(JOIN, name, null, null, null, false, false, null, players);
  }

  default Packet teamLeavePacket(String name, Collection<String> players) {
    return teamPacket(LEAVE, name, null, null, null, false, false, null, players);
  }

  Packet teamPacket(
      TeamPacketOperation operation,
      String name,
      String displayName,
      String prefix,
      String suffix,
      boolean friendlyFire,
      boolean seeFriendlyInvisibles,
      NameTagVisibility nameTagVisibility,
      Collection<String> players);

  // For legacy 1.7 players
  void removeAndAddAllTabPlayers(Player viewer);

  enum TeamPacketOperation {
    CREATE,
    REMOVE,
    UPDATE,
    JOIN,
    LEAVE
  }

  interface PlayerInfo extends Packet {
    default void addPlayerInfo(UUID uuid) {
      addPlayerInfo(uuid, null, 0, null, null);
    }

    default void addPlayerInfo(UUID uuid, int ping) {
      addPlayerInfo(uuid, uuid.toString().substring(0, 16), ping, null, null);
    }

    default void addPlayerInfo(UUID uuid, String renderedDisplayName) {
      addPlayerInfo(uuid, "|" + uuid.toString().substring(0, 15), 0, null, renderedDisplayName);
    }

    void addPlayerInfo(
        UUID uuid,
        String name,
        int ping,
        @Nullable Skin skin,
        @Nullable String renderedDisplayName);

    boolean isNotEmpty();
  }
}
