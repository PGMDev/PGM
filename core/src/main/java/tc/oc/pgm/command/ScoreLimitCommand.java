package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;

import app.ashcon.intake.Command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.util.Audience;

public final class ScoreLimitCommand {

  @Command(
      aliases = {"scorelimit", "sl"},
      desc = "Set the score limit",
      usage = "[number]",
      help = "Amount must be an integer",
      flags = "r",
      perms = Permissions.GAMEPLAY)
  public void scorelimit(Audience audience, Match match, Integer amount) {
    try {
      final ScoreMatchModule score = match.needModule(ScoreMatchModule.class);

      score.setScoreLimit(amount);
      audience.sendMessage(
          translatable(
              "match.scoreLimit.commandOutput",
              NamedTextColor.YELLOW,
              Component.text(amount).color(NamedTextColor.AQUA)));

    } catch (ModuleLoadException e) {
      audience.sendWarning(translatable("match.scoreLimit.notEnabled"));
    }
  }
}
