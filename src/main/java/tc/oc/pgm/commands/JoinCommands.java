package tc.oc.pgm.commands;


import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.param.Switch;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.commands.annotations.Text;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.teams.TeamMatchModule;

import java.util.NoSuchElementException;

@CommandContainer
public class JoinCommands {

  @Command(
          name = "join",
          aliases = {"play"},
          desc = "Joins the current match",
          descFooter = "[team] - defaults to random")
  public static void join(
      CommandSender sender,
      Match match,
      MatchPlayer player,
      @Switch(name = 'f', desc = "force") boolean force,
      @Nullable @Text String team)
  {
    JoinMatchModule jmm = match.needMatchModule(JoinMatchModule.class);
    TeamMatchModule tmm = match.getMatchModule(TeamMatchModule.class);

    Competitor chosenParty = null;

    if (team != null) {
      if (team.trim().toLowerCase().startsWith("obs")) {
        leave(player, match);
        return;
      } else if (tmm != null) {
        // player wants to join a specific team
        chosenParty = tmm.bestFuzzyMatch(team.trim());
        if (chosenParty == null)
          throw new NoSuchElementException(
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
          name = "leave",
          aliases = {"obs"},
          desc = "Leaves the current match, placing the sender on the observing team")
  public static void leave(MatchPlayer player, Match match){
    match.needMatchModule(JoinMatchModule.class).leave(player);
  }
}
