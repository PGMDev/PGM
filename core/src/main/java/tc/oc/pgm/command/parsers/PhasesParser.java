package tc.oc.pgm.command.parsers;

import static org.incendo.cloud.parser.ArgumentParseResult.failure;
import static org.incendo.cloud.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.invalidFormat;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ParserParameters;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.map.Phase;

public class PhasesParser extends StringLikeParser<CommandSender, Phase.Phases>
    implements BlockingSuggestionProvider.Strings<CommandSender> {

  public PhasesParser(CommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options);
  }

  @Override
  public ArgumentParseResult<Phase.Phases> parse(
      @NotNull CommandContext<CommandSender> context, @NotNull String text) {
    if (text.equalsIgnoreCase("*")) {
      return success(Phase.Phases.allOf());
    }

    Phase phase = Phase.of(text);
    if (phase == null) return failure(invalidFormat(text, Phase.class));
    return success(Phase.Phases.of(phase));
  }

  @Override
  public @NonNull Iterable<@NonNull String> stringSuggestions(
      @NonNull CommandContext<CommandSender> context, @NonNull CommandInput input) {
    final String next = input.peekString().toLowerCase();

    List<String> suggestions = EnumSet.allOf(Phase.class).stream()
        .map(p -> p.name().toLowerCase(Locale.ROOT))
        .filter(name -> name.startsWith(next))
        .collect(Collectors.toList());

    if ("*".startsWith(next)) suggestions.add("*");
    return suggestions;
  }
}
