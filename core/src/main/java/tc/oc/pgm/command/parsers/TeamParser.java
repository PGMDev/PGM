package tc.oc.pgm.command.parsers;

import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.Collection;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.text.TextException;

public final class TeamParser extends MatchObjectParser.Simple<Team, TeamMatchModule> {

  public TeamParser(PaperCommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options, Team.class, TeamMatchModule.class, "teams");
  }

  @Override
  protected Collection<Team> objects(TeamMatchModule module) {
    return module.getParticipatingTeams();
  }

  @Override
  protected String getName(Team obj) {
    return obj.getNameLegacy();
  }

  @Override
  protected TextException moduleNotFound() {
    return exception("command.noTeams");
  }
}
