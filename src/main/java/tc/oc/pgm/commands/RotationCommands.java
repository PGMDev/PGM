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
import tc.oc.pgm.rotation.Rotation;
import tc.oc.pgm.rotation.RotationManager;
import tc.oc.pgm.util.PrettyPaginatedResult;

public class RotationCommands {
  @Command(
      aliases = {"rotation", "rot"},
      desc = "Shows the maps in the active rotation",
      usage = "[page] [-r rotation]",
      help = "Shows all the maps that are currently in the active rotation.")
  public static void rotation(
      Audience audience,
      CommandSender sender,
      MatchManager matchManager,
      @Default("1") int page,
      @Switch('r') String rotationName)
      throws CommandException {

    RotationManager rotationManager = getRotationManager(sender, matchManager);
    Rotation rotation =
        rotationName == null
            ? rotationManager.getActiveRotation()
            : rotationManager.getRotationByName(rotationName);

    if (rotation == null) {
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.rotation.noRotation", sender));
      return;
    }
    List<PGMMap> maps = rotation.getMaps();

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
            + rotation.getName()
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

    new PrettyPaginatedResult<PGMMap>(listHeader, resultsPerPage) {
      @Override
      public String format(PGMMap map, int index) {
        index++;
        String indexString =
            rotation.getNextPosition() == index
                ? ChatColor.DARK_AQUA.toString() + index
                : String.valueOf(index);
        return (indexString) + ". " + ChatColor.RESET + map.getInfo().getShortDescription(sender);
      }
    }.display(audience, maps, page);
  }

  @Command(
      aliases = {"rotations", "rots"},
      desc = "Shows all the existing rotations.",
      help = "Shows all the existing rotations and their trigger player counts.")
  public static void rotations(
      Audience audience, CommandSender sender, MatchManager matchManager, @Default("1") int page)
      throws CommandException {

    RotationManager rotationManager = getRotationManager(sender, matchManager);

    List<Rotation> rotations = rotationManager.getRotations();
    if (rotations.isEmpty()) {
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.rotation.noRotations", sender));
      return;
    }

    int resultsPerPage = 8;
    int pages = (rotations.size() + resultsPerPage - 1) / resultsPerPage;

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

    new PrettyPaginatedResult<Rotation>(listHeader, resultsPerPage) {
      @Override
      public String format(Rotation rotation, int index) {
        String arrow =
            rotationManager.getActiveRotation().getName().equals(rotation.getName())
                ? ChatColor.GREEN + "» "
                : "» ";
        return arrow
            + ChatColor.GOLD
            + rotation.getName()
            + ChatColor.DARK_AQUA
            + " ("
            + ChatColor.AQUA
            + "Players: "
            + ChatColor.WHITE
            + rotation.getPlayers()
            + ChatColor.DARK_AQUA
            + ")";
      }
    }.display(audience, rotations, page);
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

    RotationManager rotationManager = getRotationManager(sender, matchManager);

    rotationManager.getActiveRotation().advance(positions);
    rotationManager.saveRotations();
    sender.sendMessage(
        ChatColor.WHITE
            + "["
            + ChatColor.GOLD
            + "Rotations"
            + ChatColor.WHITE
            + "] "
            + "["
            + ChatColor.AQUA
            + rotationManager.getActiveRotation().getName()
            + ChatColor.WHITE
            + "] "
            + ChatColor.GREEN
            + AllTranslations.get()
                .translate(
                    "command.rotation.skip.message",
                    sender,
                    (ChatColor.AQUA.toString() + positions + ChatColor.GREEN)));
  }

  private static RotationManager getRotationManager(CommandSender sender, MatchManager matchManager)
      throws CommandException {
    if (matchManager.getMapOrder() instanceof RotationManager)
      return (RotationManager) matchManager.getMapOrder();

    throw new CommandException(
        AllTranslations.get().translate("command.rotation.rotationsDisabled", sender));
  }
}
