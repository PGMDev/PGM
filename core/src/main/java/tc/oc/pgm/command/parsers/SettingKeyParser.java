package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.invalidFormat;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.SimpleCloudKey;
import io.leangen.geantyref.TypeToken;
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

  public static final CloudKey<SettingKey> SETTING_KEY =
      SimpleCloudKey.of("_pgm_setting_key_param_", new TypeToken<SettingKey>() {});

  @Override
  public @NonNull ArgumentParseResult<SettingKey> parse(
      final @NonNull CommandContext<CommandSender> context,
      final @NonNull Queue<String> inputQueue) {
    final String input = inputQueue.peek();
    if (input == null) {
      return failure(new NoInputProvidedException(SettingKeyParser.class, context));
    }

    SettingKey bestMatch = StringUtils.bestFuzzyMatch(input, SettingKey.class);
    if (bestMatch == null) return failure(invalidFormat(input, SettingKey.class));

    inputQueue.remove();
    // Add it to context to allow others to query for it
    context.set(SETTING_KEY, bestMatch);
    return success(bestMatch);
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