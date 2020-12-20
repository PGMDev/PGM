package tc.oc.pgm.command.graph;

import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.invalidFormat;

import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.argument.MissingArgumentException;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import app.ashcon.intake.parametric.ProvisionException;
import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

final class PartyProvider implements BukkitProvider<Party> {

  @Override
  public String getName() {
    return "team";
  }

  @Override
  public Party get(CommandSender sender, CommandArgs args, List<? extends Annotation> list)
      throws MissingArgumentException, ProvisionException {
    final String text = args.next();

    final Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match == null) {
      throw exception("command.onlyPlayers");
    }

    if (text.startsWith("obs")) {
      return match.getDefaultParty();
    }

    final TeamMatchModule teams = match.getModule(TeamMatchModule.class);
    if (teams == null) {
      throw exception("command.noTeams");
    }

    final Team team = teams.bestFuzzyMatch(text);
    if (team == null) {
      throw invalidFormat(text, Team.class, null);
    }

    return team;
  }
}
