package tc.oc.pgm.command.parsers;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

public final class MatchPlayerParser
    implements ArgumentParser<CommandSender, MatchPlayer>,
        BlockingSuggestionProvider.Strings<CommandSender> {

  private final PlayerParser parser = new PlayerParser();

  @Override
  public @NotNull ArgumentParseResult<@NotNull MatchPlayer> parse(
      @NotNull CommandContext<@NotNull CommandSender> context, @NotNull CommandInput inputQueue) {
    return parser.parse(context, inputQueue).mapParsedValue(PGM.get().getMatchManager()::getPlayer);
  }

  @Override
  public @NotNull List<@NotNull String> stringSuggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull CommandInput input) {
    return parser.stringSuggestions(context, input);
  }
}
