package tc.oc.pgm.util;

import java.util.UUID;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public class MessageSenderIdentity {

  private final UUID playerId;
  private final String name;

  public MessageSenderIdentity(Player viewer, Player player) {
    this.playerId = player.getUniqueId();
    this.name = Players.getVisibleName(viewer, player);
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public String getName() {
    return name;
  }

  public MatchPlayer getPlayer(Player viewer) {
    MatchPlayer target = PGM.get().getMatchManager().getPlayer(playerId);

    // Prevent replying to offline players
    if (target == null) return null;

    // Compare last known and current name
    String currentName = Players.getVisibleName(viewer, target.getBukkit());

    // Ensure the target is visible to the viewing sender
    boolean visible = Players.isVisible(viewer, target.getBukkit());

    if (currentName.equalsIgnoreCase(name) && visible) {
      return target;
    }

    return null;
  }
}
