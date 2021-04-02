package tc.oc.pgm.util.tablist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.reflect.ReflectionUtils;

public class TabRender {
  private final TabView view;

  private final PacketPlayOutPlayerInfo removePacket;
  private final PacketPlayOutPlayerInfo addPacket;
  private final PacketPlayOutPlayerInfo updatePacket;
  private final PacketPlayOutPlayerInfo updatePingPacket;
  private final List<Packet> deferredPackets;

  public TabRender(TabView view) {
    this.view = view;

    this.removePacket =
        this.createPlayerInfoPacket(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER);
    this.addPacket =
        this.createPlayerInfoPacket(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER);
    this.updatePacket =
        this.createPlayerInfoPacket(
            PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME);
    this.updatePingPacket =
        this.createPlayerInfoPacket(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_LATENCY);
    this.deferredPackets = new ArrayList<>();
  }

  protected static List<PacketPlayOutPlayerInfo.PlayerInfoData> getBField(
      PacketPlayOutPlayerInfo packet) {
    if (BukkitUtils.isSportPaper()) {
      return packet.b;
    } else {
      return (List<PacketPlayOutPlayerInfo.PlayerInfoData>)
          ReflectionUtils.readField(packet.getClass(), packet, List.class, "b");
    }
  }

  private String teamName(int slot) {
    return "\u0001TabView" + String.format("%03d", slot);
  }

  private void send(Packet packet) {
    NMSHacks.sendPacket(this.view.getViewer(), packet);
  }

  protected static PacketPlayOutPlayerInfo createPlayerInfoPacket(
      PacketPlayOutPlayerInfo.EnumPlayerInfoAction action) {
    PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
    if (BukkitUtils.isSportPaper()) {
      packet.a = action;
    } else {
      ReflectionUtils.setField(packet.getClass(), packet, action, "a");
    }
    return packet;
  }

  private BaseComponent[] getContent(TabEntry entry, int index) {
    return entry.getContent(this.view);
  }

  private void appendAddition(TabEntry entry, int index) {
    BaseComponent[] displayName = this.getContent(entry, index);
    getBField(this.addPacket)
        .add(
            NMSHacks.playerListPacketData(
                this.addPacket,
                entry.getId(),
                entry.getName(this.view),
                entry.getGamemode(),
                entry.getPing(),
                entry.getSkin(this.view),
                displayName));

    // Due to a client bug, display name is ignored in ADD_PLAYER packets,
    // so we have to send an UPDATE_DISPLAY_NAME afterward.
    getBField(this.updatePacket)
        .add(NMSHacks.playerListPacketData(this.updatePacket, entry.getId(), displayName));

    this.updateFakeEntity(entry, true);
  }

  private void appendRemoval(TabEntry entry) {
    getBField(this.removePacket)
        .add(NMSHacks.playerListPacketData(this.removePacket, entry.getId()));

    int entityId = entry.getFakeEntityId(this.view);
    if (entityId >= 0) {
      this.send(NMSHacks.destroyEntitiesPacket(entityId));
    }
  }

  private void leaveSlot(TabEntry entry, int index) {
    this.send(
        NMSHacks.teamLeavePacket(
            this.teamName(index), Collections.singleton(entry.getName(this.view))));
  }

  private void joinSlot(TabEntry entry, int index) {
    this.send(
        NMSHacks.teamJoinPacket(
            this.teamName(index), Collections.singleton(entry.getName(this.view))));
  }

  public void finish() {
    if (!getBField(this.removePacket).isEmpty()) this.send(this.removePacket);
    if (!getBField(this.addPacket).isEmpty()) this.send(this.addPacket);
    if (!getBField(this.updatePacket).isEmpty()) this.send(this.updatePacket);
    if (!getBField(this.updatePingPacket).isEmpty()) this.send(this.updatePingPacket);

    for (Packet packet : this.deferredPackets) {
      this.send(packet);
    }
  }

  public void changeSlot(TabEntry entry, int oldIndex, int newIndex) {
    Collection<String> names = Collections.singleton(entry.getName(this.view));
    this.send(NMSHacks.teamJoinPacket(this.teamName(newIndex), names));
  }

  public void createSlot(TabEntry entry, int index) {
    String teamName = this.teamName(index);
    this.send(
        NMSHacks.teamCreatePacket(
            teamName,
            teamName,
            "",
            "",
            false,
            false,
            Collections.singleton(entry.getName(this.view))));
    this.appendAddition(entry, index);
  }

  public void destroySlot(TabEntry entry, int index) {
    this.send(NMSHacks.teamRemovePacket(this.teamName(index)));
    this.appendRemoval(entry);
  }

  public void addEntry(TabEntry entry, int index) {
    this.joinSlot(entry, index);
    this.appendAddition(entry, index);
  }

  public void removeEntry(TabEntry entry, int index) {
    this.leaveSlot(entry, index);
    this.appendRemoval(entry);
  }

  public void refreshEntry(TabEntry entry, int index) {
    this.appendRemoval(entry);
    this.appendAddition(entry, index);
  }

  public void updateEntry(TabEntry entry, int index) {
    getBField(this.updatePacket)
        .add(
            NMSHacks.playerListPacketData(
                this.updatePacket, entry.getId(), this.getContent(entry, index)));
  }

  public void updatePing(TabEntry entry, int index) {
    getBField(this.updatePingPacket)
        .add(NMSHacks.playerListPacketData(this.updatePingPacket, entry.getId(), entry.getPing()));
  }

  public void setHeaderFooter(BaseComponent[] header, BaseComponent[] footer) {
    if (BukkitUtils.isSportPaper()) {
      view.getViewer().setPlayerListHeaderFooter(header, footer);
    } else {
      PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();
      ReflectionUtils.setField(
          packet.getClass(),
          packet,
          IChatBaseComponent.ChatSerializer.a(
              net.md_5.bungee.chat.ComponentSerializer.toString(header)),
          "a");
      ReflectionUtils.setField(
          packet.getClass(),
          packet,
          IChatBaseComponent.ChatSerializer.a(
              net.md_5.bungee.chat.ComponentSerializer.toString(footer)),
          "b");
      send(packet);
    }
  }

  public void updateFakeEntity(TabEntry entry, boolean create) {
    Player player = entry.getFakePlayer(this.view);
    if (player != null) {
      int entityId = entry.getFakeEntityId(this.view);
      if (create) {
        this.deferredPackets.add(
            NMSHacks.spawnPlayerPacket(
                entityId,
                entry.getId(),
                new Location(this.view.getViewer().getWorld(), 0, Integer.MAX_VALUE / 2, 0, 0, 0),
                player));
      } else {
        this.deferredPackets.add(NMSHacks.entityMetadataPacket(entityId, player, true));
      }
    }
  }
}
