package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.rotation.Rotation;
import tc.oc.pgm.util.PrettyPaginatedResult;

public class RotationCommands {
  @Command(
      aliases = {"rotation", "rot"},
      desc = "Shows the maps in the active rotation",
      usage = "[page]",
      help = "Shows all the maps that are currently in the active rotation.")
  public static void rotation(
      Audience audience, CommandSender sender, MatchManager matchManager, @Default("1") int page)
      throws CommandException {

    List<PGMMap> maps;
    Rotation rotation = matchManager.getRotationManager().getActiveRotation();
    if (rotation != null) maps = rotation.getMaps();
    else {
      sender.sendMessage(
          ChatColor.RED + AllTranslations.get().translate("command.rotation.noRotation", sender));
      return;
    }

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
            /*
             * This below ensures that the string remains always colored. For some reason the logic behind this de-colored
             * the text if it were to get put into a second line of chat when the header exceeded the maximum length
             * of the first one
             */
            + ChatColor.translateAlternateColorCodes('&', "&9&m-----------");

    new PrettyPaginatedResult<PGMMap>(listHeader, resultsPerPage) {
      @Override
      public String format(PGMMap map, int index) {
        index++;
        String indexString =
            matchManager.getRotationManager().getActiveRotation().getPosition() + 1 == index
                ? ChatColor.DARK_AQUA.toString() + index
                : String.valueOf(index);
        return (indexString) + ". " + ChatColor.RESET + map.getInfo().getShortDescription(sender);
      }
    }.display(audience, maps, page);
  }

  @Command(
      aliases = {
        "rotationcheckout",
        "rotcheck",
      },
      desc = "Shows the maps in the specified rotation",
      usage = "[rotation] [page]",
      help = "Shows all the maps that are currently in a specific rotation.")
  public static void rotationCheckout(
      Audience audience,
      CommandSender sender,
      MatchManager matchManager,
      String rotationName,
      @Default("1") int page)
      throws CommandException {

    List<PGMMap> maps;
    Rotation rotation = matchManager.getRotationManager().getRotationByName(rotationName);
    if (rotation != null) maps = rotation.getMaps();
    else {
      sender.sendMessage(
          ChatColor.RED
              + AllTranslations.get().translate("command.rotation.noRotationMatch", sender));
      return;
    }

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
            /*
             * This below ensures that the string remains always colored. For some reason the logic behind this de-colored
             * the text if it were to get put into a second line of chat when the header exceeded the maximum length
             * of the first one
             */
            + ChatColor.translateAlternateColorCodes('&', "&9&m-----------");

    new PrettyPaginatedResult<PGMMap>(listHeader, resultsPerPage) {
      @Override
      public String format(PGMMap map, int index) {
        index++;
        String indexString =
            rotation.getPosition() + 1 == index
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
    List<Rotation> rotations;
    if (!matchManager.getRotationManager().getRotations().isEmpty()) {
      rotations = matchManager.getRotationManager().getRotations();
    } else {
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
            matchManager
                    .getRotationManager()
                    .getActiveRotation()
                    .getName()
                    .equals(rotation.getName())
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
      CommandSender sender, MatchManager matchManager, @Default("1") int positions) {

    matchManager.getRotationManager().getActiveRotation().movePosition(positions);
    sender.sendMessage(
        ChatColor.WHITE
            + "["
            + ChatColor.GOLD
            + "Rotations"
            + ChatColor.WHITE
            + "] "
            + "["
            + ChatColor.AQUA
            + matchManager.getRotationManager().getActiveRotation().getName()
            + ChatColor.WHITE
            + "] "
            + "Skipped a total of "
            + ChatColor.AQUA
            + positions
            + ChatColor.WHITE
            + " positions.");
  }
}
