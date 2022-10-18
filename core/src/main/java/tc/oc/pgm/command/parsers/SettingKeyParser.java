package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.invalidFormat;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.StringUtils;

public final class SettingKeyParser implements ArgumentParser<CommandSender, SettingKey> {

  @Override
  public @NonNull ArgumentParseResult<SettingKey> parse(
      final @NonNull CommandContext<CommandSender> context,
      final @NonNull Queue<String> inputQueue) {
    final String input = inputQueue.peek();
    if (input == null) {
      return failure(new NoInputProvidedException(DurationParser.class, context));
    }

    SettingKey bestMatch = StringUtils.bestFuzzyMatch(input, SettingKey.class);
    if (bestMatch != null) {
      inputQueue.remove();
      return success(bestMatch);
    }

    return failure(invalidFormat(input, SettingKey.class, null));
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      final @NonNull CommandContext<CommandSender> commandContext, final @NonNull String input) {
    return Arrays.stream(SettingKey.values())
        .flatMap(
            sk ->
                sk.getAliases().stream().filter(alias -> LiquidMetal.match(alias, input)).limit(1))
        .collect(Collectors.toList());
  }
}
