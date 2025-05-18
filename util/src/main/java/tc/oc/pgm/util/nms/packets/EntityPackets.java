package tc.oc.pgm.util.nms.packets;

import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;
import static tc.oc.pgm.util.nms.Packets.TAB_PACKETS;
import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.nms.EnumPlayerInfoAction;

public interface EntityPackets {

  default int allocateEntityId() {
    return NMS_HACKS.allocateEntityId();
  }

  default FakeEntity fakeWitherSkull() {
    return new FakeEntity.Impl(allocateEntityId()) {
      @Override
      public Packet spawn(Location location, Vector velocity) {
        return spawnWitherSkull(location, entityId(), velocity);
      }
    };
  }

  default FakeEntity fakeArmorStand(@Nullable ItemStack helmet) {
    return new FakeEntity.Impl(allocateEntityId()) {
      @Override
      public Packet spawn(Location location, Vector velocity) {
        Packet spawn = spawnArmorStand(location, entityId(), velocity);
        return helmet != null ? Packet.of(spawn, wearHead(helmet)) : spawn;
      }
    };
  }

  default Packet spawnFreezeEntity(Player player, int entityId, boolean legacy) {
    if (legacy) {
      Location loc = player.getLocation().add(0, 0.286, 0);
      if (loc.getY() < -64) {
        loc.setY(-64);
        player.teleport(loc);
      }
      return spawnWitherSkull(loc, entityId, new Vector());
    } else {
      Location loc = player.getLocation().subtract(0, 1.1, 0);
      return spawnArmorStand(loc, entityId, new Vector());
    }
  }

  default FakeEntity fakePlayer(Player original, ChatColor color) {
    UUID uuid = UUID.randomUUID();
    String team = uuid.toString().substring(0, 16);
    // Add color to void matching real name. Cut to avoid exceeding 16 chars
    String playerName = color + StringUtils.substring(original.getName(), 0, 14);
    String suffix = StringUtils.substring(original.getName(), 14, 16);
    return new FakeEntity.Impl(allocateEntityId()) {
      @Override
      public Packet spawn(Location location, Vector velocity) {
        var tabInfo = TAB_PACKETS.createPlayerInfoPacket(EnumPlayerInfoAction.ADD_PLAYER);
        tabInfo.addPlayerInfo(uuid, playerName, 0, PLAYER_UTILS.getPlayerSkin(original), null);

        return Packet.of(
            tabInfo,
            TAB_PACKETS.spawnPlayerPacket(entityId(), uuid, location, original),
            TAB_PACKETS.teamCreatePacket(
                team, team, color + "", suffix, false, false, List.of(playerName)));
      }

      @Override
      public Packet teleport(Location location) {
        return Packet.of(super.teleport(location), updateHeadRotation(entityId(), location));
      }

      @Override
      public Packet destroy() {
        var info = TAB_PACKETS.createPlayerInfoPacket(EnumPlayerInfoAction.REMOVE_PLAYER);
        info.addPlayerInfo(uuid);
        return Packet.of(info, super.destroy(), TAB_PACKETS.teamRemovePacket(team));
      }
    };
  }

  Packet spawnArmorStand(Location loc, int entityId, Vector velocity);

  Packet spawnWitherSkull(Location location, int entityId, Vector velocity);

  Packet destroyEntitiesPacket(int... entityIds);

  Packet teleportEntityPacket(int entityId, Location location);

  Packet updateHeadRotation(int entityId, Location location);

  Packet entityMount(int entityId, int vehicleId);

  Packet entityEquipment(
      int entityId, ItemStack helmet, ItemStack chest, ItemStack leggings, ItemStack boots);

  Packet entityHeadEquipment(int entityId, ItemStack helmet);

  Packet entityMetadataPacket(int entityId, Entity entity, boolean complete);
}
