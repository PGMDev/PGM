package tc.oc.pgm.command.parsers;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import tc.oc.pgm.command.util.CommandKeys;
import tc.oc.pgm.rotation.pools.MapPoolType;
import tc.oc.pgm.rotation.pools.Rotation;

public final class RotationParser
    implements ArgumentParser<CommandSender, Rotation>,
        BlockingSuggestionProvider.Strings<CommandSender>,
        ParseUtils {

  private final MapPoolParser POOL_PARSER = new MapPoolParser();

  @Override
  public @NonNull ArgumentParseResult<Rotation> parse(
      final @NonNull CommandContext<CommandSender> context,
      final @NonNull CommandInput inputQueue) {
    context.set(CommandKeys.POOL_TYPE, MapPoolType.ORDERED);
    return map(POOL_PARSER.parse(context, inputQueue), p -> (Rotation) p);
  }

  @Override
  public @NonNull List<@NonNull String> stringSuggestions(
      final @NonNull CommandContext<CommandSender> context, final @NonNull CommandInput input) {
    context.set(CommandKeys.POOL_TYPE, MapPoolType.ORDERED);
    return POOL_PARSER.stringSuggestions(context, input);
  }
}
