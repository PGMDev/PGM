package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;

import app.ashcon.intake.Command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.util.Audience;

public final class ScoreLimitCommand {

  @Command(
      aliases = {"score", "sl"},
      desc = "Set the score limit",
      usage = "[amount]",
      help = "Amount must be an integer",
      flags = "r",
      perms = Permissions.GAMEPLAY)
  public void scorelimit(Audience audience, Match match, Integer amount) {

    final ScoreMatchModule score = match.needModule(ScoreMatchModule.class);

    score.setScoreLimit(amount);
    audience.sendMessage(
        translatable(
            "match.scoreLimit.commandOutput",
            NamedTextColor.YELLOW,
            Component.text(amount).color(NamedTextColor.AQUA)));
  }
}
