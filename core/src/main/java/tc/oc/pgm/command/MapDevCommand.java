package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;

import app.ashcon.intake.Command;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import java.util.Optional;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.variables.Variable;

public class MapDevCommand {

  @Command(
      aliases = {"variables"},
      desc = "Inspect pgm variables",
      perms = Permissions.DEBUG)
  public void showVariables(
      Match match, MatchPlayer sender, @Fallback(Type.NULL) Optional<Player> target) {
    MatchPlayer filterable = target.map(match::getPlayer).orElse(sender);

    sender.sendMessage(text("Showing variables for " + filterable.getNameLegacy() + ":"));
    for (Variable<?> v : match.getFeatureContext().getAll(Variable.class)) {
      sender.sendMessage(text(v.getId() + ": " + v.getValue(filterable)));
    }
  }
}
