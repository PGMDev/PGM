package tc.oc.pgm.platform.v1_20_6.packets;

import static net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER;
import static net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME;
import static net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY;
import static net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.NameTagVisibility;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.platform.v1_20_6.Skins;
import tc.oc.pgm.util.nms.EnumPlayerInfoAction;
import tc.oc.pgm.util.nms.packets.Packet;
import tc.oc.pgm.util.nms.packets.TabPackets;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.skin.Skin;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernTabPackets implements TabPackets {

  @Override
  public PlayerInfo createPlayerInfoPacket(EnumPlayerInfoAction action) {
    return action == EnumPlayerInfoAction.REMOVE_PLAYER
        ? new RemovePlayerInfo()
        : new ModernPlayerInfo(action);
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

  Team.Visibility[] NMS_TEAM_VISIBILITY = Team.Visibility.values();

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
    return new ModernPacket<>(
        switch (operation) {
          case CREATE, UPDATE -> {
            var team = new PlayerTeam(new Scoreboard(), name);
            if (displayName != null) team.setDisplayName(Component.literal(displayName));
            if (prefix != null) team.setPlayerPrefix(Component.literal(prefix));
            if (suffix != null) team.setPlayerSuffix(Component.literal(suffix));
            team.setAllowFriendlyFire(friendlyFire);
            team.setSeeFriendlyInvisibles(seeFriendlyInvisibles);
            team.getPlayers().addAll(players);

            yield ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(
                team, operation == TeamPacketOperation.CREATE);
          }
          case REMOVE -> ClientboundSetPlayerTeamPacket.createRemovePacket(
              new PlayerTeam(null, name));
          case JOIN -> ClientboundSetPlayerTeamPacket.createMultiplePlayerPacket(
              new PlayerTeam(null, name), players, ClientboundSetPlayerTeamPacket.Action.ADD);
          case LEAVE -> ClientboundSetPlayerTeamPacket.createMultiplePlayerPacket(
              new PlayerTeam(null, name), players, ClientboundSetPlayerTeamPacket.Action.REMOVE);
        });
  }

  @Override
  public void removeAndAddAllTabPlayers(Player viewer) {}

  private static EnumSet<Action> toNmsAction(EnumPlayerInfoAction action) {
    return switch (action) {
      case ADD_PLAYER -> EnumSet.of(ADD_PLAYER, UPDATE_LISTED, UPDATE_LATENCY, UPDATE_DISPLAY_NAME);
      case UPDATE_LATENCY -> EnumSet.of(UPDATE_LATENCY);
      case UPDATE_DISPLAY_NAME -> EnumSet.of(UPDATE_DISPLAY_NAME);
      case REMOVE_PLAYER -> throw new IllegalArgumentException("Unsupported action: " + action);
    };
  }

  static class RemovePlayerInfo extends ModernPacket<ClientboundPlayerInfoRemovePacket>
      implements PlayerInfo {
    public RemovePlayerInfo() {
      super(new ClientboundPlayerInfoRemovePacket(new ArrayList<>()));
    }

    @Override
    public void addPlayerInfo(
        UUID uuid,
        String name,
        int ping,
        @Nullable Skin skin,
        @Nullable String renderedDisplayName) {
      packet.profileIds().add(uuid);
    }

    @Override
    public boolean isNotEmpty() {
      return !packet.profileIds().isEmpty();
    }
  }

  static class ModernPlayerInfo extends ModernPacket<ClientboundPlayerInfoUpdatePacket>
      implements PlayerInfo {

    public ModernPlayerInfo(EnumPlayerInfoAction action) {
      super(new ClientboundPlayerInfoUpdatePacket(toNmsAction(action), new ArrayList<Entry>()));
    }

    @Override
    public void addPlayerInfo(
        UUID uuid, String name, int ping, @Nullable Skin skin, @Nullable String jsonName) {

      GameProfile profile = new GameProfile(uuid, name);
      if (skin != null) Skins.toProfile(profile, skin);

      Component nmsComponent = jsonName == null
          ? null
          : Component.Serializer.fromJson(jsonName, HolderLookup.Provider.create(Stream.of()));

      packet
          .entries()
          .add(new Entry(uuid, profile, true, ping, GameType.SURVIVAL, nmsComponent, null));
    }

    @Override
    public boolean isNotEmpty() {
      return !packet.entries().isEmpty();
    }
  }
}
