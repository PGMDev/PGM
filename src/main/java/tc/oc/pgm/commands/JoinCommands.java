package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.teams.TeamMatchModule;

public class JoinCommands {

  @Command(
      aliases = {"join", "play"},
      desc = "Joins the current match",
      usage = "[team] - defaults to random",
      flags = "f",
      perms = Permissions.JOIN)
  public static void join(
      CommandSender sender,
      Match match,
      MatchPlayer player,
      @Switch('f') boolean force,
      @Nullable @Text String team)
      throws CommandException {
    JoinMatchModule jmm = match.needModule(JoinMatchModule.class);
    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);

    Competitor chosenParty = null;

    if (team != null) {
      if (team.trim().toLowerCase().startsWith("obs")) {
        leave(player, match);
        return;
      } else if (tmm != null) {
        // player wants to join a specific team
        chosenParty = tmm.bestFuzzyMatch(team.trim());
        if (chosenParty == null)
          throw new CommandException(
              AllTranslations.get().translate("command.teamNotFound", sender));
      }
    }

    if (sender.hasPermission(Permissions.JOIN_FORCE) && force) {
      jmm.forceJoin(player, chosenParty);
    } else {
      jmm.join(player, chosenParty);
    }
  }

  @Command(
      aliases = {"leave", "obs"},
      desc = "Leaves the current match, placing the sender on the observing team",
      perms = Permissions.LEAVE)
  public static void leave(MatchPlayer player, Match match) throws CommandException {
    match.needModule(JoinMatchModule.class).leave(player);
  }
}
