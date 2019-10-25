package tc.oc.pgm.tablist;

import java.util.Comparator;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import tc.oc.pgm.Config;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.server.Permissions;

/**
 * The order that players are listed for a given viewer. Roughly speaking, the order is: 1. viewer
 * 2. friends 3. staff 4. other flair ranks 5. alphabetical
 */
public class PlayerOrder implements Comparator<MatchPlayer> {
  private final MatchPlayer viewer;

  PlayerOrder(MatchPlayer viewer) {
    this.viewer = viewer;
  }

  @Override
  public int compare(MatchPlayer ma, MatchPlayer mb) {
    if (this.viewer == ma) {
      return -1;
    } else if (this.viewer == mb) {
      return 1;
    }

    Player a = ma.getBukkit();
    Player b = mb.getBukkit();
    Player viewer = this.viewer.getBukkit();

    boolean aStaff = a.hasPermission(Permissions.STAFF);
    boolean bStaff = b.hasPermission(Permissions.STAFF);

    // Staff take priority
    if (aStaff && !bStaff) {
      return -1;
    } else if (bStaff && !aStaff) {
      return 1;
    }

    // If players have different permissions, the player with the highest ranked perm
    // that the other one does't have is first. Disguised players effectively have no perms.
    for (Permission permission : Config.PlayerList.getPermsByPriority()) {
      boolean aPerm = a.hasPermission(permission);
      boolean bPerm = b.hasPermission(permission);

      if (aPerm && !bPerm) {
        return -1;
      } else if (bPerm && !aPerm) {
        return 1;
      }
    }

    // All else equal, order the players alphabetically
    return a.getName(viewer).compareToIgnoreCase(b.getName(viewer));
  }
}
