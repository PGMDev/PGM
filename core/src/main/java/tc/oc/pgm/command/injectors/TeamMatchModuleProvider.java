package tc.oc.pgm.command.injectors;

import static tc.oc.pgm.util.text.TextException.exception;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.text.TextException;

public final class TeamMatchModuleProvider extends MatchObjectProvider<TeamMatchModule> {

  @Override
  protected TeamMatchModule get(Match match) {
    return match.getModule(TeamMatchModule.class);
  }

  @Override
  protected TextException missingException() {
    return exception("command.noTeams");
  }
}
