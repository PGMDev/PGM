package tc.oc.pgm.command;

import static tc.oc.pgm.util.text.TextException.exception;

import app.ashcon.intake.Command;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.command.graph.Sender;

public final class FinishCommand {

  @Command(
      aliases = {"finish", "end"},
      desc = "End the match",
      usage = "[competitor]",
      perms = Permissions.STOP)
  public void end(Sender sender, @Nullable Party team) {
    if (!sender.getMatch().finish(team instanceof Competitor ? (Competitor) team : null)) {
      throw exception("admin.end.unknownError");
    }
  }
}
