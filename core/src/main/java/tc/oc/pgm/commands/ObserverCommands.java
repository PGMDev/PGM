package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.observers.ObserverToolsMatchModule;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

public class ObserverCommands {

  @Command(
      aliases = {"tools", "observertools", "ot"},
      desc = "Open the observer tool menu")
  public static void openObserverToolMenu(CommandSender sender, MatchPlayer player)
      throws CommandException {
    if (player.isObserving()) {
      final ObserverToolsMatchModule tools =
          player.getMatch().getModule(ObserverToolsMatchModule.class);
      if (tools != null) {
        tools.openMenu(player);
      }
    } else {
      throw new CommandException(
          ComponentRenderers.toLegacyText(
              new PersonalizedTranslatable("setting.observersOnly"), sender));
    }
  }
}
