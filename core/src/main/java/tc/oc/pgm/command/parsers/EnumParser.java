package tc.oc.pgm.command.parsers;

import static org.incendo.cloud.parser.ArgumentParseResult.failure;
import static org.incendo.cloud.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.invalidFormat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import tc.oc.pgm.util.Aliased;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.StreamUtils;
import tc.oc.pgm.util.StringUtils;

public class EnumParser<E extends Enum<E>>
    implements ArgumentParser<CommandSender, E>, BlockingSuggestionProvider.Strings<CommandSender> {

  protected final Class<E> enumClass;
  private final boolean isMultiAliased;
  protected final @Nullable CloudKey<E> key;

  public EnumParser(Class<E> enumClass) {
    this(enumClass, null);
  }

  /**
   * Create an enum parser for a specific type of enum
   *
   * @param enumClass The class of the enum
   * @param key An optional key to store the result of parsing to
   */
  public EnumParser(Class<E> enumClass, @Nullable CloudKey<E> key) {
    this.enumClass = enumClass;
    this.isMultiAliased = Aliased.class.isAssignableFrom(enumClass);
    this.key = key;
  }

  @Override
  public @NonNull ArgumentParseResult<E> parse(
      final @NonNull CommandContext<CommandSender> context,
      final @NonNull CommandInput inputQueue) {
    final String input = inputQueue.peekString();

    E bestMatch = bestMatch(context, input);
    if (bestMatch == null) return failure(invalidFormat(input, enumClass));

    inputQueue.readString();
    if (key != null) context.set(key, bestMatch);
    return success(bestMatch);
  }

  @SuppressWarnings("unchecked")
  protected E bestMatch(CommandContext<CommandSender> context, String input) {
    if (isMultiAliased)
      return (E)
          StringUtils.bestMultiFuzzyMatch(input, options(context).map(e -> (Aliased) e).iterator());
    return StringUtils.bestFuzzyMatch(input, options(context).iterator(), this::stringify);
  }

  @Override
  public @NonNull List<@NonNull String> stringSuggestions(
      final @NonNull CommandContext<CommandSender> context, final @NonNull CommandInput input) {
    return filteredOptions(context, input.readString()).collect(Collectors.toList());
  }

  protected Stream<E> options(CommandContext<CommandSender> context) {
    return Arrays.stream(enumClass.getEnumConstants());
  }

  protected Stream<String> filteredOptions(CommandContext<CommandSender> context, String input) {
    // Suggestion provider for Aliased enum (may contain multiple aliases)
    if (isMultiAliased) {
      return options(context)
          .flatMap(
              e ->
                  StreamUtils.of((Aliased) e)
                      .filter(name -> LiquidMetal.match(name, input))
                      .limit(1)); // Keep first matching alias, if any
    }

    return options(context).map(this::stringify).filter(name -> LiquidMetal.match(name, input));
  }

  protected String stringify(E e) {
    return e.toString();
  }
}
