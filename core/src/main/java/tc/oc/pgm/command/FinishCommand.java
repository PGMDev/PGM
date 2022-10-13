package tc.oc.pgm.command;

import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.teams.Team;

public final class FinishCommand {

  @CommandMethod("finish|end [team]")
  @CommandDescription("End the match")
  @CommandPermission(Permissions.STOP)
  public void end(Match match, @Argument("team") Team team) {
    if (!match.finish(team)) {
      throw exception("admin.end.unknownError");
    }
  }
}
