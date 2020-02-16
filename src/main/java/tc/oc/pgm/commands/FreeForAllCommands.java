package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Switch;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.ffa.FreeForAllMatchModule;

public class FreeForAllCommands {

  @Command(
      aliases = {"min"},
      desc = "Change the minimum number of players required to start the match.",
      usage = "(default | <min-players>)",
      perms = Permissions.RESIZE)
  public static void min(CommandSender sender, Match match, String min) throws CommandException {
    FreeForAllMatchModule ffa = getFfaModule(sender, match);

    if ("default".equals(min)) {
      ffa.setMinPlayers(null);
    } else {
      int minPlayers = Integer.parseInt(min);
      if (minPlayers < 0) throw new CommandException("min-players cannot be less than 0");
      ffa.setMinPlayers(minPlayers);
    }

    sender.sendMessage(
        ChatColor.WHITE + "Minimum players is now " + ChatColor.AQUA + ffa.getMinPlayers());
  }

  @Command(
      aliases = {"max", "size"},
      desc = "Change the maximum number of players allowed to participate in the match.",
      usage = "(default | [-p max-players] [-o max-overfill])",
      flags = "po",
      perms = Permissions.RESIZE)
  public static void max(
      CommandSender sender,
      Match match,
      Optional<String> max,
      @Fallback(Type.NULL) @Switch('p') Integer maxPlayers,
      @Fallback(Type.NULL) @Switch('o') Integer maxOverfill)
      throws CommandException {
    FreeForAllMatchModule ffa = getFfaModule(sender, match);

    if (max.isPresent() && "default".equals(max.get())) {
      ffa.setMaxPlayers(null, null);
    } else {
      if (maxPlayers == null && maxOverfill == null) {
        throw new CommandException(
            AllTranslations.get()
                .translate(
                    "commands.incorrectUsage",
                    sender,
                    "<team> (default | [-p max-players] [-o max-overfill])"));
      }

      maxPlayers = maxPlayers == null ? ffa.getMaxPlayers() : maxPlayers;
      maxOverfill = maxOverfill == null ? maxPlayers : maxOverfill;

      if (maxPlayers < 0) throw new CommandException("max-players cannot be less than 0");

      if (maxOverfill < maxPlayers)
        throw new CommandException("max-overfill cannot be less than max-players");

      ffa.setMaxPlayers(maxPlayers, maxOverfill);
    }

    sender.sendMessage(
        ChatColor.WHITE
            + "Maximum players is now "
            + ChatColor.AQUA
            + ffa.getMaxPlayers()
            + ChatColor.WHITE
            + " and overfill is "
            + ChatColor.AQUA
            + ffa.getMaxOverfill());
  }

  private static FreeForAllMatchModule getFfaModule(CommandSender sender, Match match)
      throws CommandException {
    FreeForAllMatchModule ffaModule = match.getModule(FreeForAllMatchModule.class);
    if (ffaModule == null) {
      throw new CommandException(
          AllTranslations.get()
              .translate(
                  "command.moduleNotFound", sender, FreeForAllMatchModule.class.getSimpleName()));
    }
    return ffaModule;
  }
}
