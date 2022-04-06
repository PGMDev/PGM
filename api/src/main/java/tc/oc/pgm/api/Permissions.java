package tc.oc.pgm.api;

import com.google.common.collect.ImmutableMap;
import java.util.stream.Stream;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

/** This is a hard-coded list of permissions, that add the basic functionality. */
public interface Permissions {

  // Root permission node
  String ROOT = "pgm";

  // Root permission node for groups
  String GROUP = ROOT + ".group";

  // Individual permission nodes
  String START = ROOT + ".start"; // Start and cycle matches
  String STOP = ROOT + ".stop"; // Stop matches and restart the server
  String SETNEXT = ROOT + ".setnext"; // Change the rotation and maps
  String ADMINCHAT = ROOT + ".adminchat"; // Secret chat with other operators
  String GAMEPLAY = ROOT + ".gameplay"; // Edit gameplay such as time limits, destroyables, modes
  String RESIZE = ROOT + ".resize"; // Resize the number of players per match
  String JOIN = ROOT + ".join"; // Allowed to join a match as a participant
  String JOIN_CHOOSE = JOIN + ".choose"; // Can choose which team to join
  String EXTRA_VOTE = JOIN + ".extravote"; // Extra map voting power
  String JOIN_FULL = ROOT + ".full"; // Can join a team or server if it is full
  String JOIN_FORCE = JOIN + ".force"; // Can force other players onto teams
  String LEAVE = ROOT + ".leave"; // Can join observers willingly
  String DEFUSE = ROOT + ".defuse"; // Defuse tnt from observers using shears
  String DEBUG = ROOT + ".debug"; // Errors from map loading and debug commands
  String STAFF = ROOT + ".staff"; // Considered apart of the staff team
  String RELOAD = ROOT + ".reload"; // Reload the PGM configuration
  String KICK = ROOT + ".kick"; // Access to the /kick command
  String WARN = ROOT + ".warn"; // Access to the /warn command
  String MUTE = ROOT + ".mute"; // Access to the /mute command
  String BAN = ROOT + ".ban"; // Access to the /ban command
  String FREEZE = ROOT + ".freeze"; // Access to the /freeze command
  String VANISH = ROOT + ".vanish"; // Access to /vanish command

  String MAPMAKER = GROUP + ".mapmaker"; // Permission group for mapmakers, defined in config.yml

  // Role-specific permission nodes
  Permission DEFAULT =
      new Permission(
          "pgm.default",
          PermissionDefault.TRUE,
          new ImmutableMap.Builder<String, Boolean>().put(JOIN, true).put(LEAVE, true).build());

  Permission PREMIUM =
      new Permission(
          "pgm.premium",
          PermissionDefault.FALSE,
          new ImmutableMap.Builder<String, Boolean>()
              .putAll(DEFAULT.getChildren())
              .put(DEFAULT.getName(), true)
              .put(JOIN_CHOOSE, true)
              .put(JOIN_FULL, true)
              .put(EXTRA_VOTE, true)
              .build());

  Permission MODERATOR =
      new Permission(
          "pgm.mod",
          PermissionDefault.FALSE,
          new ImmutableMap.Builder<String, Boolean>()
              .putAll(PREMIUM.getChildren())
              .put(PREMIUM.getName(), true)
              .put(START, true)
              .put(STOP, true)
              .put(SETNEXT, true)
              .put(ADMINCHAT, true)
              .put(RESIZE, true)
              .put(JOIN_FORCE, true)
              .put(DEFUSE, true)
              .put(STAFF, true)
              .put(KICK, true)
              .put(WARN, true)
              .put(MUTE, true)
              .put(BAN, true)
              .put(FREEZE, true)
              .put(VANISH, true)
              .build());

  Permission DEVELOPER =
      new Permission(
          "pgm.dev",
          PermissionDefault.FALSE,
          new ImmutableMap.Builder<String, Boolean>()
              .putAll(MODERATOR.getChildren())
              .put(MODERATOR.getName(), true)
              .put(GAMEPLAY, true)
              .put(DEBUG, true)
              .put(RELOAD, true)
              .build());

  Permission ALL =
      new Permission(
          "pgm.*",
          PermissionDefault.OP,
          new ImmutableMap.Builder<String, Boolean>()
              .putAll(DEVELOPER.getChildren())
              .put(DEVELOPER.getName(), true)
              .build());

  static void registerAll() {
    Stream.of(DEFAULT, PREMIUM, MODERATOR, DEVELOPER, ALL).forEachOrdered(Permissions::register);
  }

  static Permission register(Permission permission) {
    try {
      PGM.get().getServer().getPluginManager().addPermission(permission);
    } catch (Throwable t) {
      // No-op, the permission was already registered
    }
    return permission;
  }
}
