package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public final class DurationParser implements ArgumentParser<CommandSender, Duration> {

  @Override
  public @NonNull ArgumentParseResult<Duration> parse(
      final @NonNull CommandContext<CommandSender> context,
      final @NonNull Queue<String> inputQueue) {
    final String input = inputQueue.peek();
    if (input == null) {
      return failure(new NoInputProvidedException(DurationParser.class, context));
    }
    try {
      Duration result = TextParser.parseDuration(input);
      inputQueue.remove();
      return success(result);
    } catch (TextException e) {
      return failure(e);
    }
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      final @NonNull CommandContext<CommandSender> commandContext, final @NonNull String input) {
    char[] chars = input.toLowerCase(Locale.ROOT).toCharArray();

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
        .filter(unit -> !input.contains(unit))
        .map(unit -> input + unit)
        .collect(Collectors.toList());
  }
}
