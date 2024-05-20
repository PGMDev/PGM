package tc.oc.pgm.command.parsers;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.command.util.CommandKeys;
import tc.oc.pgm.rotation.pools.MapPoolType;
import tc.oc.pgm.rotation.pools.Rotation;

public final class RotationParser
    implements ArgumentParser<CommandSender, Rotation>,
        BlockingSuggestionProvider.Strings<CommandSender> {

  private final MapPoolParser POOL_PARSER = new MapPoolParser();

  @Override
  public @NotNull ArgumentParseResult<Rotation> parse(
      final @NotNull CommandContext<CommandSender> context,
      final @NotNull CommandInput inputQueue) {
    context.set(CommandKeys.POOL_TYPE, MapPoolType.ORDERED);
    return POOL_PARSER.parse(context, inputQueue).mapParsedValue(p -> (Rotation) p);
  }

  @Override
  public @NotNull List<@NotNull String> stringSuggestions(
      final @NotNull CommandContext<CommandSender> context, final @NotNull CommandInput input) {
    context.set(CommandKeys.POOL_TYPE, MapPoolType.ORDERED);
    return POOL_PARSER.stringSuggestions(context, input);
  }
}
