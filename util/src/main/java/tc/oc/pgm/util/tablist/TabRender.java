package tc.oc.pgm.util.tablist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.nms.EnumPlayerInfoAction;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.text.TextTranslations;

public class TabRender {
  private final TabView view;

  private final Object removePacket;
  private final Object addPacket;
  private final Object updatePacket;
  private final Object updatePingPacket;
  private final List<Object> deferredPackets;

  public TabRender(TabView view) {
    this.view = view;

    this.removePacket = NMSHacks.createPlayerInfoPacket(EnumPlayerInfoAction.REMOVE_PLAYER);
    this.addPacket = NMSHacks.createPlayerInfoPacket(EnumPlayerInfoAction.ADD_PLAYER);
    this.updatePacket = NMSHacks.createPlayerInfoPacket(EnumPlayerInfoAction.UPDATE_DISPLAY_NAME);
    this.updatePingPacket = NMSHacks.createPlayerInfoPacket(EnumPlayerInfoAction.UPDATE_LATENCY);
    this.deferredPackets = new ArrayList<>();
  }

  private String teamName(int slot) {
    return "\u0001TabView" + String.format("%03d", slot);
  }

  private void send(Object packet) {
    NMSHacks.sendPacket(this.view.getViewer(), packet);
  }

  private String getJson(TabEntry entry) {
    return TextTranslations.toMinecraftGson(entry.getContent(this.view), this.view.getViewer());
  }

  private void appendAddition(TabEntry entry, int index) {
    String renderedDisplayName = this.getJson(entry);
    NMSHacks.addPlayerInfoToPacket(
        this.addPacket,
        entry.getId(),
        entry.getName(this.view),
        entry.getGamemode(),
        entry.getPing(),
        entry.getSkin(this.view),
        renderedDisplayName);

    // Due to a client bug, display name is ignored in ADD_PLAYER packets,
    // so we have to send an UPDATE_DISPLAY_NAME afterward.
    NMSHacks.addPlayerInfoToPacket(this.updatePacket, entry.getId(), renderedDisplayName);

    this.updateFakeEntity(entry, true);
  }

  private void appendRemoval(TabEntry entry) {
    NMSHacks.addPlayerInfoToPacket(this.removePacket, entry.getId());

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
    if (NMSHacks.playerInfoDataListNotEmpty(this.removePacket)) this.send(this.removePacket);
    if (NMSHacks.playerInfoDataListNotEmpty(this.addPacket)) this.send(this.addPacket);
    if (NMSHacks.playerInfoDataListNotEmpty(this.updatePacket)) this.send(this.updatePacket);
    if (NMSHacks.playerInfoDataListNotEmpty(this.updatePingPacket))
      this.send(this.updatePingPacket);

    for (Object packet : this.deferredPackets) {
      this.send(packet);
    }
  }

  public void changeSlot(TabEntry entry, int oldIndex, int newIndex) {
    Collection<String> names = Collections.singleton(entry.getName(this.view));
    this.send(NMSHacks.teamJoinPacket(this.teamName(newIndex), names));
  }

  public void createSlot(TabEntry entry, int index) {
    String teamName = this.teamName(index);
    Collection<String> players = Collections.singleton(entry.getName(this.view));
    this.send(NMSHacks.teamCreatePacket(teamName, teamName, "", "", false, false, players));
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
    NMSHacks.addPlayerInfoToPacket(this.updatePacket, entry.getId(), this.getJson(entry));
  }

  public void updatePing(TabEntry entry, int index) {
    NMSHacks.addPlayerInfoToPacket(this.updatePingPacket, entry.getId(), entry.getPing());
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
