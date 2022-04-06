package tc.oc.pgm.util.tablist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.text.TextTranslations;

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
        NMSHacks.createPlayerInfoPacket(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER);
    this.addPacket =
        NMSHacks.createPlayerInfoPacket(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER);
    this.updatePacket =
        NMSHacks.createPlayerInfoPacket(
            PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME);
    this.updatePingPacket =
        NMSHacks.createPlayerInfoPacket(
            PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_LATENCY);
    this.deferredPackets = new ArrayList<>();
  }

  private String teamName(int slot) {
    return "\u0001TabView" + String.format("%03d", slot);
  }

  private void send(Packet packet) {
    NMSHacks.sendPacket(this.view.getViewer(), packet);
  }

  private String getJson(TabEntry entry) {
    return TextTranslations.toMinecraftGson(entry.getContent(this.view), this.view.getViewer());
  }

  private void appendAddition(TabEntry entry, int index) {
    String renderedDisplayName = this.getJson(entry);
    NMSHacks.getPlayerInfoDataList(this.addPacket)
        .add(
            NMSHacks.playerListPacketData(
                this.addPacket,
                entry.getId(),
                entry.getName(this.view),
                entry.getGamemode(),
                entry.getPing(),
                entry.getSkin(this.view),
                renderedDisplayName));

    // Due to a client bug, display name is ignored in ADD_PLAYER packets,
    // so we have to send an UPDATE_DISPLAY_NAME afterward.
    NMSHacks.getPlayerInfoDataList(this.updatePacket)
        .add(NMSHacks.playerListPacketData(this.updatePacket, entry.getId(), renderedDisplayName));

    this.updateFakeEntity(entry, true);
  }

  private void appendRemoval(TabEntry entry) {
    NMSHacks.getPlayerInfoDataList(this.removePacket)
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
    if (!NMSHacks.getPlayerInfoDataList(this.removePacket).isEmpty()) this.send(this.removePacket);
    if (!NMSHacks.getPlayerInfoDataList(this.addPacket).isEmpty()) this.send(this.addPacket);
    if (!NMSHacks.getPlayerInfoDataList(this.updatePacket).isEmpty()) this.send(this.updatePacket);
    if (!NMSHacks.getPlayerInfoDataList(this.updatePingPacket).isEmpty())
      this.send(this.updatePingPacket);

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
    NMSHacks.getPlayerInfoDataList(this.updatePacket)
        .add(NMSHacks.playerListPacketData(this.updatePacket, entry.getId(), this.getJson(entry)));
  }

  public void updatePing(TabEntry entry, int index) {
    NMSHacks.getPlayerInfoDataList(this.updatePingPacket)
        .add(NMSHacks.playerListPacketData(this.updatePingPacket, entry.getId(), entry.getPing()));
  }

  public void setHeaderFooter(Component header, Component footer) {
    Audience.get(view.getViewer()).sendPlayerListHeaderAndFooter(header, footer);
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
