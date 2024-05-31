package tc.oc.pgm.command.parsers;

import static org.incendo.cloud.parser.ArgumentParseResult.failure;
import static org.incendo.cloud.parser.ArgumentParseResult.success;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public final class DurationParser
    implements ArgumentParser<CommandSender, Duration>,
        BlockingSuggestionProvider.Strings<CommandSender> {

  @Override
  public @NonNull ArgumentParseResult<Duration> parse(
      final @NonNull CommandContext<CommandSender> context,
      final @NonNull CommandInput inputQueue) {
    final String input = inputQueue.peekString();
    try {
      Duration result = TextParser.parseDuration(input);
      inputQueue.readString();
      return success(result);
    } catch (TextException e) {
      return failure(e);
    }
  }

  @Override
  public @NonNull List<@NonNull String> stringSuggestions(
      final @NonNull CommandContext<CommandSender> commandContext,
      final @NonNull CommandInput input) {
    final String next = input.readString();
    char[] chars = next.toLowerCase(Locale.ROOT).toCharArray();

    if (chars.length == 0) {
      return IntStream.range(1, 10).mapToObj(Integer::toString).collect(Collectors.toList());
    }

    char last = chars[chars.length - 1];

    // 1d_, 5d4m_, etc
    if (Character.isLetter(last)) {
      return Collections.emptyList();
    }

    // 1d5_, 5d4m2_, etc
    return Stream.of("d", "h", "m", "s")
        .filter(unit -> !next.contains(unit))
        .map(unit -> next + unit)
        .collect(Collectors.toList());
  }
}
