package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import com.google.common.collect.Range;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public final class FreeForAllCommand {

  @Command(
      aliases = {"min"},
      desc = "Set the min players on a team",
      usage = "<team> (reset | <min-players>)",
      perms = Permissions.RESIZE)
  public void min(Audience audience, Match match, String minPlayers) {
    final FreeForAllMatchModule ffa = getFfa(match);
    if (minPlayers.equalsIgnoreCase("reset")) {
      ffa.setMaxPlayers(null, null);
    } else {
      ffa.setMinPlayers(TextParser.parseInteger(minPlayers, Range.atLeast(0)));
    }

    audience.sendMessage(
        Component.translatable(
            "match.resize.max",
            Component.translatable("match.info.players", NamedTextColor.YELLOW),
            Component.text(ffa.getMinPlayers(), NamedTextColor.AQUA)));
  }

  @Command(
      aliases = {"size"},
      desc = "Set the max players",
      usage = "(reset | <max-players) [max-overfill]",
      perms = Permissions.RESIZE)
  public void max(Audience audience, Match match, String maxPlayers, @Nullable String maxOverfill) {
    final FreeForAllMatchModule ffa = getFfa(match);
    if (maxPlayers.equalsIgnoreCase("reset")) {
      ffa.setMaxPlayers(null, null);
    } else {
      final int max = TextParser.parseInteger(maxPlayers, Range.atLeast(ffa.getMinPlayers()));
      final int overfill =
          maxOverfill == null
              ? (int) Math.ceil(1.25 * max)
              : TextParser.parseInteger(maxOverfill, Range.atLeast(max));

      ffa.setMaxPlayers(max, overfill);
    }

    audience.sendMessage(
        Component.translatable(
            "match.resize.max",
            Component.translatable("match.info.players", NamedTextColor.YELLOW),
            Component.text(ffa.getMaxPlayers(), NamedTextColor.AQUA)));
  }

  private FreeForAllMatchModule getFfa(Match match) {
    final FreeForAllMatchModule ffa = match.getModule(FreeForAllMatchModule.class);
    if (ffa == null) {
      throw TextException.of("command.moduleNotFound", Component.text("free-for-all"));
    }
    return ffa;
  }
}
