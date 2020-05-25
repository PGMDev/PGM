package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import javax.annotation.Nullable;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.util.text.TextException;

public final class FinishCommand {

  @Command(
      aliases = {"finish", "end"},
      desc = "End the match",
      usage = "[competitor]",
      perms = Permissions.STOP)
  public void end(Match match, @Nullable Party team) {
    if (!match.finish(team instanceof Competitor ? (Competitor) team : null)) {
      throw TextException.of("admin.end.unknownError");
    }
  }
}
