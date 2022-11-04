package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.variables.Variable;

public class MapDevCommand {

  @CommandMethod("variables [target]")
  @CommandDescription("Inspect variables for a player")
  @CommandPermission(Permissions.DEBUG)
  public void showVariables(
      Match match, @NotNull MatchPlayer sender, @Argument("target") MatchPlayer target) {
    MatchPlayer filterable = target == null ? sender : target;

    sender.sendMessage(text("Showing variables for " + filterable.getNameLegacy() + ":"));
    for (Variable<?> v : match.getFeatureContext().getAll(Variable.class)) {
      sender.sendMessage(text(v.getId() + ": " + v.getValue(filterable)));
    }
  }
}
