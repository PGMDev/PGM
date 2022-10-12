package tc.oc.pgm.command;

import static tc.oc.pgm.util.text.TextException.exception;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Maybe;
import app.ashcon.intake.parametric.annotation.Text;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;

public final class FinishCommand {

  @Command(
      aliases = {"finish", "end"},
      desc = "End the match",
      usage = "[competitor]",
      perms = Permissions.STOP)
  public void end(Match match, @Text @Maybe Party team) {
    if (!match.finish(team instanceof Competitor ? (Competitor) team : null)) {
      throw exception("admin.end.unknownError");
    }
  }
}
