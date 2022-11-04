package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.invalidFormat;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.StringUtils;

public final class SettingValueParser implements ArgumentParser<CommandSender, SettingValue> {

  @Override
  public @NonNull ArgumentParseResult<SettingValue> parse(
      final @NonNull CommandContext<CommandSender> context,
      final @NonNull Queue<String> inputQueue) {
    final String input = inputQueue.peek();
    if (input == null) {
      return failure(new NoInputProvidedException(SettingValueParser.class, context));
    }

    SettingKey key = context.get(SettingKeyParser.SETTING_KEY);

    SettingValue value =
        StringUtils.bestFuzzyMatch(
            input, Iterators.forArray(key.getPossibleValues()), SettingValue::getName);

    if (value == null) return failure(invalidFormat(input, SettingValue.class));

    inputQueue.remove();
    return success(value);
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      final @NonNull CommandContext<CommandSender> context, final @NonNull String input) {
    SettingKey key = context.get(SettingKeyParser.SETTING_KEY);

    return Arrays.stream(key.getPossibleValues())
        .map(SettingValue::getName)
        .filter(val -> LiquidMetal.match(val, input))
        .collect(Collectors.toList());
  }
}
