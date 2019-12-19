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
import tc.oc.pgm.rotation.FixedPGMMapOrder;
import tc.oc.pgm.rotation.FixedPGMMapOrderManager;
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

    if (matchManager.getMapOrder() instanceof FixedPGMMapOrderManager) {
      FixedPGMMapOrderManager fixedPGMMapOrderManager =
          (FixedPGMMapOrderManager) matchManager.getMapOrder();
      FixedPGMMapOrder rotation = fixedPGMMapOrderManager.getActiveRotation();

      List<PGMMap> maps;
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
    } else {
      sender.sendMessage(
          ChatColor.RED
              + AllTranslations.get().translate("command.rotation.rotationsDisabled", sender));
    }
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

    if (matchManager.getMapOrder() instanceof FixedPGMMapOrderManager) {
      List<PGMMap> maps;
      FixedPGMMapOrderManager fixedPGMMapOrderManager =
          (FixedPGMMapOrderManager) matchManager.getMapOrder();
      FixedPGMMapOrder rotation = fixedPGMMapOrderManager.getRotationByName(rotationName);

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
    } else {
      sender.sendMessage(
          ChatColor.RED
              + AllTranslations.get().translate("command.rotation.rotationsDisabled", sender));
    }
  }

  @Command(
      aliases = {"rotations", "rots"},
      desc = "Shows all the existing rotations.",
      help = "Shows all the existing rotations and their trigger player counts.")
  public static void rotations(
      Audience audience, CommandSender sender, MatchManager matchManager, @Default("1") int page)
      throws CommandException {

    if (matchManager.getMapOrder() instanceof FixedPGMMapOrderManager) {
      FixedPGMMapOrderManager fixedPGMMapOrderManager =
          (FixedPGMMapOrderManager) matchManager.getMapOrder();

      List<FixedPGMMapOrder> rotations;
      if (!fixedPGMMapOrderManager.getRotations().isEmpty()) {
        rotations = fixedPGMMapOrderManager.getRotations();
      } else {
        sender.sendMessage(
            ChatColor.RED
                + AllTranslations.get().translate("command.rotation.noRotations", sender));
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

      new PrettyPaginatedResult<FixedPGMMapOrder>(listHeader, resultsPerPage) {
        @Override
        public String format(FixedPGMMapOrder rotation, int index) {
          String arrow =
              fixedPGMMapOrderManager.getActiveRotation().getName().equals(rotation.getName())
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
    } else {
      sender.sendMessage(
          ChatColor.RED
              + AllTranslations.get().translate("command.rotation.rotationsDisabled", sender));
    }
  }

  @Command(
      aliases = {"skip"},
      desc = "Skips one or more maps from the current rotation.",
      usage = "[positions]",
      perms = Permissions.SETNEXT)
  public static void skip(
      CommandSender sender, MatchManager matchManager, @Default("1") int positions) {

    if (positions < 0) {
      sender.sendMessage(
          ChatColor.RED
              + AllTranslations.get().translate("command.rotation.skip.noNegative", sender));
      return;
    }

    if (matchManager.getMapOrder() instanceof FixedPGMMapOrderManager) {
      FixedPGMMapOrderManager fixedPGMMapOrderManager =
          (FixedPGMMapOrderManager) matchManager.getMapOrder();

      fixedPGMMapOrderManager.getActiveRotation().movePosition(positions);
      fixedPGMMapOrderManager.saveCurrentPosition();
      sender.sendMessage(
          ChatColor.WHITE
              + "["
              + ChatColor.GOLD
              + "Rotations"
              + ChatColor.WHITE
              + "] "
              + "["
              + ChatColor.AQUA
              + fixedPGMMapOrderManager.getActiveRotation().getName()
              + ChatColor.WHITE
              + "] "
              + ChatColor.GREEN
              + AllTranslations.get()
                  .translate(
                      "command.rotation.skip.message",
                      sender,
                      (ChatColor.AQUA.toString() + positions + ChatColor.GREEN)));
    } else {
      sender.sendMessage(
          ChatColor.RED
              + AllTranslations.get().translate("command.rotation.rotationsDisabled", sender));
    }
  }
}
