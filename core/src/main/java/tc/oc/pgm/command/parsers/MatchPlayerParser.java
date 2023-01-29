package tc.oc.pgm.command.parsers;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import java.util.List;
import java.util.Queue;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public final class MatchPlayerParser implements ArgumentParser<CommandSender, MatchPlayer> {

  private final PlayerParser parser = new PlayerParser();

  @Override
  public @NotNull ArgumentParseResult<@NotNull MatchPlayer> parse(
      @NotNull CommandContext<@NotNull CommandSender> context,
      @NotNull Queue<@NotNull String> inputQueue) {
    return parser.parse(context, inputQueue).mapParsedValue(PGM.get().getMatchManager()::getPlayer);
  }

  @Override
  public @NotNull List<@NotNull String> suggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull String input) {
    return parser.suggestions(context, input);
  }
}
