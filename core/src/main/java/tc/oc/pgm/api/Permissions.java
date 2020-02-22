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
  String JOIN_FULL = ROOT + ".full"; // Can join a team or server if it is full
  String JOIN_FORCE = JOIN + ".force"; // Can force other players onto teams
  String LEAVE = ROOT + ".leave"; // Can join observers willingly
  String DEFUSE = ROOT + ".defuse"; // Defuse tnt from observers using shears
  String DEBUG = ROOT + ".debug"; // Errors from map loading and debug commands
  String STAFF = ROOT + ".staff"; // Considered apart of the staff team
  String RELOAD = ROOT + ".reload"; // Reload the PGM configuration

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
              .put(JOIN_CHOOSE, true)
              .put(JOIN_FULL, true)
              .build());

  Permission MODERATOR =
      new Permission(
          "pgm.mod",
          PermissionDefault.FALSE,
          new ImmutableMap.Builder<String, Boolean>()
              .putAll(PREMIUM.getChildren())
              .put(START, true)
              .put(STOP, true)
              .put(SETNEXT, true)
              .put(ADMINCHAT, true)
              .put(RESIZE, true)
              .put(JOIN_FORCE, true)
              .put(DEFUSE, true)
              .put(STAFF, true)
              .build());

  Permission DEVELOPER =
      new Permission(
          "pgm.dev",
          PermissionDefault.FALSE,
          new ImmutableMap.Builder<String, Boolean>()
              .putAll(MODERATOR.getChildren())
              .put(GAMEPLAY, true)
              .put(DEBUG, true)
              .put(RELOAD, true)
              .build());

  Permission ALL = new Permission("pgm.*", PermissionDefault.OP, DEVELOPER.getChildren());

  // Global-disable permission nodes
  Permission DISABLE =
      new Permission(
          "pgm.disable",
          PermissionDefault.NOT_OP,
          new ImmutableMap.Builder<String, Boolean>()
              .put("worldedit.navigation.ceiling", false)
              .put("worldedit.navigation.up", false)
              .put("worldedit.calc", false)
              .put("bukkit.command.kill", false)
              .put("bukkit.command.me", false)
              .put("bukkit.command.tell", false)
              .put("commandbook.pong", false)
              .put("commandbook.speed.flight", false)
              .put("commandbook.speed.walk", false)
              .build());

  // Party-specific permission nodes
  Permission PARTICIPANT =
      new Permission(
          "pgm.participant",
          PermissionDefault.FALSE,
          new ImmutableMap.Builder<String, Boolean>()
              .putAll(DISABLE.getChildren())
              .put("worldedit.navigation.jumpto.tool", false)
              .put("worldedit.navigation.thru.tool", false)
              .put("commandbook.teleport", false)
              .build());
  Permission OBSERVER =
      new Permission(
          "pgm.observer",
          PermissionDefault.FALSE,
          new ImmutableMap.Builder<String, Boolean>()
              .putAll(DISABLE.getChildren())
              .put("worldedit.navigation.*", true)
              .put("commandbook.teleport", true)
              .build());

  static void registerAll() {
    Stream.of(DEFAULT, PREMIUM, MODERATOR, DEVELOPER, ALL, PARTICIPANT, OBSERVER)
        .forEachOrdered(Permissions::register);
  }

  static Permission register(Permission permission) {
    PGM.get().getServer().getPluginManager().addPermission(permission);
    return permission;
  }

  static Permission register(String node, PermissionDefault def) {
    Permission permission = PGM.get().getServer().getPluginManager().getPermission(node);
    if (permission == null) {
      permission = register(new Permission(node, def));
    }
    return permission;
  }
}
