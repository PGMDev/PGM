package tc.oc.pgm.command;

import static tc.oc.pgm.util.text.TextException.exception;

import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.teams.Team;

public final class FinishCommand {

  @Command("finish|end [team]")
  @CommandDescription("End the match")
  @Permission(Permissions.STOP)
  public void end(Match match, @Argument("team") Team team) {
    if (!match.finish(team)) {
      throw exception("admin.end.unknownError");
    }
  }
}
