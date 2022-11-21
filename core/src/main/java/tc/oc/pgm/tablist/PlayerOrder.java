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
    if (this.viewer == ma) return -1;
    else if (this.viewer == mb) return 1;

    Player a = ma.getBukkit();
    Player b = mb.getBukkit();
    Player viewer = this.viewer.getBukkit();

    boolean isStaff = viewer.hasPermission(Permissions.STAFF);

    // Friends are always first (assuming they see nicks)
    boolean aFriend = Integration.isFriend(viewer, a);
    boolean bFriend = Integration.isFriend(viewer, b);
    if (aFriend && !bFriend) return -1;
    else if (bFriend && !aFriend) return 1;

    String aNick = aFriend || isStaff ? null : Integration.getNick(a);
    String bNick = bFriend || isStaff ? null : Integration.getNick(b);
    boolean aStaff = aNick == null && a.hasPermission(Permissions.STAFF);
    boolean bStaff = bNick == null && b.hasPermission(Permissions.STAFF);

    // Staff take priority, as long as nick is visible
    if (aStaff && !bStaff) return -1;
    else if (bStaff && !aStaff) return 1;

    // Short-circuit skipping groupcheck if both are nicked
    if (aNick != null && bNick != null) return aNick.compareToIgnoreCase(bNick);

    // If players have different permissions, the player with the highest ranked perm
    // that the other one does't have is first. Disguised players effectively have no perms.
    for (Config.Group group : PGM.get().getConfiguration().getGroups()) {
      Permission permission = group.getPermission();
      if (!group.getId().equalsIgnoreCase("default")) {
        boolean aPerm = aNick == null && a.hasPermission(permission);
        boolean bPerm = bNick == null && b.hasPermission(permission);

        if (aPerm && !bPerm) return -1;
        else if (bPerm && !aPerm) return 1;
      }
    }
    if (aNick == null) aNick = a.getName();
    if (bNick == null) bNick = b.getName();
    // All else equal, order the players alphabetically
    return aNick.compareToIgnoreCase(bNick);
  }
}
