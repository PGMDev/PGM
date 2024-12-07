package tc.oc.pgm.spawns.states;

import java.util.function.Function;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public class StatePermissions {

  private final Function<Config.Group, Permission> groupFunction;
  private PermissionAttachment permissionAttachment;

  private StatePermissions(Function<Config.Group, Permission> groupFunction) {
    this.groupFunction = groupFunction;
  }

  public void givePermission(MatchPlayer player) {
    var bukkit = player.getBukkit();
    permissionAttachment = bukkit.addAttachment(PGM.get());
    for (Config.Group group : PGM.get().getConfiguration().getGroups()) {
      if (bukkit.hasPermission(group.getPermission())) {
        permissionAttachment.setPermission(groupFunction.apply(group), true);
      }
    }
  }

  public void revokePermission(MatchPlayer player) {
    if (permissionAttachment != null) player.getBukkit().removeAttachment(permissionAttachment);
  }

  public static class Observer extends StatePermissions {
    public Observer() {
      super(Config.Group::getObserverPermission);
    }
  }

  public static class Participant extends StatePermissions {
    public Participant() {
      super(Config.Group::getParticipantPermission);
    }
  }
}
