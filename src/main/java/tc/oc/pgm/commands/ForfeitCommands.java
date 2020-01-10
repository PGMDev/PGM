package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Switch;
import java.util.Optional;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.modules.ForfeitMatchModule;

public class ForfeitCommands {

  @Command(
      aliases = {"forfeit"},
      desc = "Vote to forfeit the match")
  public static void forfeitMatch(
      CommandSender sender, Match match, MatchPlayer player, @Switch('t') boolean toggle) {
    Optional<ForfeitMatchModule> forfeit = match.getModule(ForfeitMatchModule.class);
    if (forfeit.isPresent()) {
      ForfeitMatchModule fmm = forfeit.get();

      if (sender.hasPermission(Permissions.STAFF) && toggle) {
        boolean newStatus = fmm.setEnabled(!fmm.isEnabled());

        sender.sendMessage(
            new PersonalizedTranslatable(
                    newStatus ? "command.forfeit.enable" : "command.forfeit.disable")
                .getPersonalizedText()
                .color(ChatColor.GRAY));
      } else {
        if (fmm.isEnabled()) {
          if (player.isObserving()) {
            player.sendWarning(new PersonalizedTranslatable("command.forfeit.observers"));
          } else {
            fmm.forfeit(player, sender);
          }
        } else {
          player.sendWarning(new PersonalizedTranslatable("command.forfeit.notEnabled"));
        }
      }
    }
  }
}
