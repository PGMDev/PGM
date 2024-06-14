package tc.oc.pgm.platform.v1_20_6.packets;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import java.util.Collection;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.NameTagVisibility;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.nms.EnumPlayerInfoAction;
import tc.oc.pgm.util.nms.packets.Packet;
import tc.oc.pgm.util.nms.packets.TabPackets;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.skin.Skin;

@Supports(value = PAPER, minVersion = "1.20.6")
public class NoOpTabPackets implements TabPackets {

  @Override
  public PlayerInfo createPlayerInfoPacket(EnumPlayerInfoAction action) {
    return new PlayerInfo() {
      @Override
      public void addPlayerInfo(
          UUID uuid,
          String name,
          GameMode gamemode,
          int ping,
          @Nullable Skin skin,
          @Nullable String renderedDisplayName) {}

      @Override
      public boolean isNotEmpty() {
        return false;
      }

      @Override
      public void send(Player viewer) {}

      @Override
      public void sendToViewers(Entity entity, boolean excludeSpectators) {}
    };
  }

  @Override
  public Packet spawnPlayerPacket(int entityId, UUID uuid, Location location, Player player) {
    return new Packet() {
      @Override
      public void send(Player viewer) {}

      @Override
      public void sendToViewers(Entity entity, boolean excludeSpectators) {}
    };
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
    return new Packet() {
      @Override
      public void send(Player viewer) {}

      @Override
      public void sendToViewers(Entity entity, boolean excludeSpectators) {}
    };
  }

  @Override
  public void removeAndAddAllTabPlayers(Player viewer) {}
}
