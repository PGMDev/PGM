package tc.oc.pgm.tablist;

import java.util.Comparator;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.nick.NickProvider;

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

    NickProvider nick = PGM.get().getNickRegistry().getProvider();
    boolean aNicked = nick.getNick(a.getUniqueId()).isPresent();
    boolean bNicked = nick.getNick(b.getUniqueId()).isPresent();

    boolean aStaff = a.hasPermission(Permissions.STAFF) && !aNicked;
    boolean bStaff = b.hasPermission(Permissions.STAFF) && !bNicked;

    boolean aFriend = Integration.isFriend(viewer, a);
    boolean bFriend = Integration.isFriend(viewer, b);

    // Friends are always first :)
    if (aFriend && !bFriend) {
      return -1;
    } else if (bFriend && !aFriend) {
      return 1;
    }

    if (aNicked && !bNicked) {
      return nick.getPlayerName(a).compareToIgnoreCase(b.getName(viewer));
    } else if (bNicked && !aNicked) {
      return a.getName(viewer).compareToIgnoreCase(nick.getPlayerName(b));
    }

    // Staff take priority, unless they are nicked
    if (aStaff && !bStaff) {
      return -1;
    } else if (bStaff && !aStaff) {
      return 1;
    }

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
