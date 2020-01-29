package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import org.bukkit.command.CommandSender;
import tc.oc.component.render.ComponentRenderers;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.observers.ObserverToolsMatchModule;

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
              new PersonalizedTranslatable("observer.command.wrongmode"), sender));
    }
  }
}
