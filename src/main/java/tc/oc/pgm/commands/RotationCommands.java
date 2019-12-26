package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.rotation.MapPool;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.Rotation;
import tc.oc.pgm.util.PrettyPaginatedResult;

public class RotationCommands {
  @Command(
      aliases = {"rotation", "rot", "pool"},
      desc = "Shows the maps in the active rotation",
      usage = "[page] [-r rotation]",
      help = "Shows all the maps that are currently in the active rotation.")
  public static void rotation(
      Audience audience,
      CommandSender sender,
      MatchManager matchManager,
      @Default("1") int page,
      @Switch('r') String rotationName,
      @Switch('p') String poolName)
      throws CommandException {
    if (rotationName != null) poolName = rotationName;

    MapPoolManager mapPoolManager = getMapPoolManager(sender, matchManager);
    MapPool mapPool =
        poolName == null
            ? mapPoolManager.getActiveMapPool()
            : mapPoolManager.getMapPoolByName(poolName);

    if (mapPool == null) {
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.rotation.noRotation", sender));
      return;
    }
    List<PGMMap> maps = mapPool.getMaps();

    int resultsPerPage = 8;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    String listHeader =
        ChatColor.BLUE.toString()
            + ChatColor.STRIKETHROUGH
            + "-----------"
            + ChatColor.RESET
            + " "
            + AllTranslations.get().translate("command.rotation.currentRotation.title", sender)
            + ChatColor.DARK_AQUA
            + " ("
            + ChatColor.AQUA
            + mapPool.getName()
            + ChatColor.DARK_AQUA
            + ")"
            + " ("
            + ChatColor.AQUA
            + page
            + ChatColor.DARK_AQUA
            + " of "
            + ChatColor.AQUA
            + pages
            + ChatColor.DARK_AQUA
            + ") "
            + ChatColor.translateAlternateColorCodes('&', "&9&m-----------");
    int nextPos = mapPool instanceof Rotation ? ((Rotation) mapPool).getNextPosition() : -1;

    new PrettyPaginatedResult<PGMMap>(listHeader, resultsPerPage) {
      @Override
      public String format(PGMMap map, int index) {
        index++;
        String indexString =
            nextPos == index ? ChatColor.DARK_AQUA.toString() + index : String.valueOf(index);
        return (indexString) + ". " + ChatColor.RESET + map.getInfo().getShortDescription(sender);
      }
    }.display(audience, maps, page);
  }

  @Command(
      aliases = {"rotations", "rots", "pools"},
      desc = "Shows all the existing rotations.",
      help = "Shows all the existing rotations and their trigger player counts.")
  public static void rotations(
      Audience audience, CommandSender sender, MatchManager matchManager, @Default("1") int page)
      throws CommandException {

    MapPoolManager mapPoolManager = getMapPoolManager(sender, matchManager);

    List<MapPool> mapPools = mapPoolManager.getMapPools();
    if (mapPools.isEmpty()) {
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.rotation.noRotations", sender));
      return;
    }

    int resultsPerPage = 8;
    int pages = (mapPools.size() + resultsPerPage - 1) / resultsPerPage;

    String listHeader =
        ChatColor.BLUE.toString()
            + ChatColor.STRIKETHROUGH
            + "-----------"
            + ChatColor.RESET
            + " "
            + AllTranslations.get().translate("command.rotation.rotationList.title", sender)
            + ChatColor.DARK_AQUA
            + " ("
            + ChatColor.AQUA
            + page
            + ChatColor.DARK_AQUA
            + " of "
            + ChatColor.AQUA
            + pages
            + ChatColor.DARK_AQUA
            + ") "
            + ChatColor.translateAlternateColorCodes('&', "&9&m-----------");

    new PrettyPaginatedResult<MapPool>(listHeader, resultsPerPage) {
      @Override
      public String format(MapPool mapPool, int index) {
        String arrow =
            mapPoolManager.getActiveMapPool().getName().equals(mapPool.getName())
                ? ChatColor.GREEN + "» "
                : "» ";
        return arrow
            + ChatColor.GOLD
            + mapPool.getName()
            + ChatColor.DARK_AQUA
            + " ("
            + ChatColor.AQUA
            + "Players: "
            + ChatColor.WHITE
            + mapPool.getPlayers()
            + ChatColor.DARK_AQUA
            + ")";
      }
    }.display(audience, mapPools, page);
  }

  @Command(
      aliases = {"skip"},
      desc = "Skips one or more maps from the current rotation.",
      usage = "[positions]",
      perms = Permissions.SETNEXT)
  public static void skip(
      CommandSender sender, MatchManager matchManager, @Default("1") int positions)
      throws CommandException {

    if (positions < 0) {
      sender.sendMessage(
          ChatColor.RED
              + AllTranslations.get().translate("command.rotation.skip.noNegative", sender));
      return;
    }

    MapPool pool = getMapPoolManager(sender, matchManager).getActiveMapPool();
    if (!(pool instanceof Rotation)) {
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.rotation.noRotation", sender));
      return;
    }

    ((Rotation) pool).advance(positions);
    sender.sendMessage(
        ChatColor.WHITE
            + "["
            + ChatColor.GOLD
            + "Rotations"
            + ChatColor.WHITE
            + "] "
            + "["
            + ChatColor.AQUA
            + pool.getName()
            + ChatColor.WHITE
            + "] "
            + ChatColor.GREEN
            + AllTranslations.get()
                .translate(
                    "command.rotation.skip.message",
                    sender,
                    (ChatColor.AQUA.toString() + positions + ChatColor.GREEN)));
  }

  private static MapPoolManager getMapPoolManager(CommandSender sender, MatchManager matchManager)
      throws CommandException {
    if (matchManager.getMapOrder() instanceof MapPoolManager)
      return (MapPoolManager) matchManager.getMapOrder();

    throw new CommandException(
        AllTranslations.get().translate("command.rotation.rotationsDisabled", sender));
  }
}
