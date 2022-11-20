package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.invalidFormat;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.keys.CloudKey;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import tc.oc.pgm.util.Aliased;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.StreamUtils;
import tc.oc.pgm.util.StringUtils;

public class EnumParser<E extends Enum<E>> implements ArgumentParser<CommandSender, E> {

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
      final @NonNull Queue<String> inputQueue) {
    final String input = inputQueue.peek();
    if (input == null) {
      return failure(new NoInputProvidedException(EnumParser.class, context));
    }

    E bestMatch = bestMatch(context, input);
    if (bestMatch == null) return failure(invalidFormat(input, enumClass));

    inputQueue.remove();
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
  public @NonNull List<@NonNull String> suggestions(
      final @NonNull CommandContext<CommandSender> context, final @NonNull String input) {
    return filteredOptions(context, input).collect(Collectors.toList());
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
