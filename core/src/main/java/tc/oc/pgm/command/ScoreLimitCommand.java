package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Switch;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.util.Audience;

public final class ScoreLimitCommand {

  @Command(
      aliases = {"scorelimit", "sl"},
      desc = "Set the score limit",
      usage = "[number]",
      help = "Score must be an integer",
      flags = "r",
      perms = Permissions.GAMEPLAY)
  public void scorelimit(
      Audience audience, Match match, @Nullable Integer amount, @Switch('r') boolean reset) {
    final ScoreMatchModule score = match.getModule(ScoreMatchModule.class);
    if (score != null) {
      if (amount != null) {
        score.setScoreLimit(amount);
      } else if (reset) {
        score.resetScoreLimit();
      }
      audience.sendMessage(
          translatable(
              "match.scoreLimit.commandOutput",
              NamedTextColor.YELLOW,
              Component.text(score.getScoreLimit()).color(NamedTextColor.AQUA)));
      return;
    }
    audience.sendWarning(translatable("match.scoreLimit.notEnabled"));
  }
}
