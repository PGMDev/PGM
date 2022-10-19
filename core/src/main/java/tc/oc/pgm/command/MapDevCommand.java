package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Maybe;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.variables.Variable;

public class MapDevCommand {

  @Command(
      aliases = {"variables"},
      desc = "Inspect variables for a player",
      perms = Permissions.DEBUG)
  public void showVariables(Match match, MatchPlayer sender, @Maybe Player target) {
    MatchPlayer filterable = target == null ? sender : match.getPlayer(target);

    sender.sendMessage(text("Showing variables for " + filterable.getNameLegacy() + ":"));
    for (Variable<?> v : match.getFeatureContext().getAll(Variable.class)) {
      sender.sendMessage(text(v.getId() + ": " + v.getValue(filterable)));
    }
  }
}
