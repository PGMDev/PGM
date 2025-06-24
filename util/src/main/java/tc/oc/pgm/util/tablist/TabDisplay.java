package tc.oc.pgm.util.tablist;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.nms.Packets.TAB_PACKETS;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.nms.EnumPlayerInfoAction;
import tc.oc.pgm.util.nms.packets.Packet;
import tc.oc.pgm.util.nms.packets.TabPackets;

/**
 * Render arbitrary strings to the TAB list AKA player list. Before this is used with a player,
 * their list should be initialized with {@link #setup}. This will disable default updates for the
 * list, and it is assumed that nothing else will make changes to the player's list while TabDisplay
 * is working with it.
 *
 * <p>This class works by filling the list with unique invisible player names consisting of
 * formatting codes and invisible characters. Each of those players is added to a unique team, and
 * the team prefix and suffix are used to show text in the player's slot in the list.
 */
public class TabDisplay {
  // Used as the ping value for all slots
  private static final int PING = 9999;

  // Number of rows in the player list, which is always 20 AFAIK
  public static final int HEIGHT = 20;

  // Maximum number of columns that the player list will show
  public static final int MAX_WIDTH = 10;

  // Width and total size of the list
  private final int width, slots;

  private final Player viewer;
  private final String[] rendered;

  // Cached packets used to setup and tear down the player list
  private final Packet[] teamCreatePackets;
  private final Packet[] teamRemovePackets;

  private final TabPackets.PlayerInfo listAddPacket;
  private final TabPackets.PlayerInfo listRemovePacket;

  public TabDisplay(Player viewer, int width) {
    // Number of columns is maxPlayers/rows rounded up
    this.width = width;
    this.slots = this.width * HEIGHT;

    this.viewer = viewer;
    this.rendered = new String[this.slots];

    this.teamCreatePackets = new Packet[this.slots];
    this.teamRemovePackets = new Packet[this.slots];

    this.listAddPacket = TAB_PACKETS.createPlayerInfoPacket(EnumPlayerInfoAction.ADD_PLAYER);
    this.listRemovePacket = TAB_PACKETS.createPlayerInfoPacket(EnumPlayerInfoAction.REMOVE_PLAYER);

    SlotBuilder slots = new SlotBuilder();
    for (int slot = 0; slot < this.slots; ++slot) {
      String name = slots.getPlayerName(slot);
      var renderedPlayerName = text(name);

      String teamName = this.slotTeamName(slot);
      this.teamCreatePackets[slot] = TAB_PACKETS.teamCreatePacket(
          teamName, teamName, "", "", false, false, Collections.singleton(name));
      this.teamRemovePackets[slot] = TAB_PACKETS.teamRemovePacket(teamName);
      UUID uuid = UUID.randomUUID();

      listAddPacket.addPlayerInfo(uuid, name, PING, null, renderedPlayerName);
      listRemovePacket.addPlayerInfo(uuid, renderedPlayerName);
    }
  }

  private int slotIndex(int x, int y) {
    return y * this.width + x;
  }

  private String slotTeamName(int slot) {
    return "TabDisplay" + String.format("%03d", slot);
  }

  public void set(int slot, String text) {
    if (rendered != null && !Objects.equals(text, rendered[slot])) {
      rendered[slot] = text;

      String[] split = StringUtils.splitIntoTeamPrefixAndSuffix(text);
      String name = this.slotTeamName(slot);
      TAB_PACKETS.teamUpdatePacket(name, name, split[0], split[1], false, false).send(viewer);
    }
  }

  /** Set the text for the slot at the given position for the given viewer */
  public void set(int x, int y, String text) {
    this.set(this.slotIndex(x, y), text);
  }

  public void setup() {
    this.listAddPacket.send(this.viewer);
    for (int slot = 0; slot < this.slots; ++slot) {
      this.teamCreatePackets[slot].send(viewer);
    }

    Arrays.fill(rendered, "");

    // Force removing and re-adding all players, because tab list is FIFO in 1.7, re-adding
    // players makes them append at the end
    TAB_PACKETS.removeAndAddAllTabPlayers(viewer);
  }

  public void tearDown() {
    for (int slot = 0; slot < this.slots; ++slot) {
      this.teamRemovePackets[slot].send(viewer);
    }
    this.listRemovePacket.send(viewer);
  }

  private static class SlotBuilder {
    private static final char COLOR_CODE = '§';
    // A no-space character for 1.7 clients
    private static final char NO_SPACE = '\u1FFF';
    // Chat color amount
    private static final int COLORS = 16;

    private final char[] builder =
        new char[] {COLOR_CODE, '0', NO_SPACE, COLOR_CODE, '0', NO_SPACE};

    /**
     * Creates an unique, invisible name for the slot. Uses a combination of color-codes and an
     * invisible character for 1.7 clients, in a hex-like conversion. If _ is the empty char, Slot 3
     * (0x03) becomes §0_§3_, while Slot 25 (0x19) becomes §1_§9_
     *
     * @param slot The slot to create a unique player name for
     * @return The base component array of invisible characters
     */
    public String getPlayerName(int slot) {
      assert slot < 0xFF; // Numbers higher than this are not supported.
      builder[4] = Character.forDigit(slot % COLORS, COLORS);
      builder[1] = Character.forDigit((slot / COLORS) % COLORS, COLORS);
      return new String(builder);
    }
  }
}
