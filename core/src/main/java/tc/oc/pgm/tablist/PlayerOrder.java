package tc.oc.pgm.tablist;

import java.util.Comparator;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * The default order that players are listed for a given viewer. Roughly speaking, the order is: 1.
 * viewer 2. friends 3. staff 4. other flair ranks 5. alphabetical
 */
public class PlayerOrder implements Comparator<MatchPlayer> {

  private final MatchPlayer viewer;

  public PlayerOrder(MatchPlayer viewer) {
    this.viewer = viewer;
  }

  public MatchPlayer getViewer() {
    return viewer;
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

    // Check if the viewer has staff permissions
    boolean isStaff = viewer.hasPermission(Permissions.STAFF);

    // Check if viewer is in a squad with the player
    boolean aSquad = Integration.areInSquad(viewer, a);
    boolean bSquad = Integration.areInSquad(viewer, b);

    if (aSquad && !bSquad) return -1;
    else if (bSquad && !aSquad) return 1;

    // Check if viewer is friends with the player
    boolean aFriend = Integration.isFriend(viewer, a);
    boolean bFriend = Integration.isFriend(viewer, b);

    if (aFriend && !bFriend) return -1;
    else if (bFriend && !aFriend) return 1;

    String aNick = aFriend || isStaff ? null : Integration.getNick(a);
    String bNick = bFriend || isStaff ? null : Integration.getNick(b);

    // Check if 'a' and 'b' are staff members (only counts if they're not nicked)
    boolean aStaff = aNick == null && a.hasPermission(Permissions.STAFF);
    boolean bStaff = bNick == null && b.hasPermission(Permissions.STAFF);

    if (aStaff && !bStaff) return -1;
    else if (bStaff && !aStaff) return 1;

    // Compare the nicknames of 'a' and 'b' if both are nicked, skip group permission check
    if (aNick != null && bNick != null) return aNick.compareToIgnoreCase(bNick);

    // If players have different permissions, the player with the highest ranked perm
    // that the other one does't have is first. Disguised players effectively have no perms.
    for (Config.Group group : PGM.get().getConfiguration().getGroups()) {
      Permission permission = group.getPermission();
      boolean aPerm = a.hasPermission(permission);
      boolean bPerm = b.hasPermission(permission);

      if (aPerm && !bPerm) {
        return -1;
      } else if (bPerm && !aPerm) {
        return 1;
      }
    }

    // All else equal, order the players alphabetically
    return a.getName().compareToIgnoreCase(b.getName());
  }
}
