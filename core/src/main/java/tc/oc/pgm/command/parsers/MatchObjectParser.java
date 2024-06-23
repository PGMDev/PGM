package tc.oc.pgm.command.parsers;

import static net.kyori.adventure.text.Component.text;
import static org.incendo.cloud.parser.ArgumentParseResult.failure;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.invalidFormat;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ParserParameters;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.command.util.CommandUtils;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.StreamUtils;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.text.TextException;

/**
 * Generic parser for match-time objects, eg: teams, classes, modes, etc. Automatically handles
 * getting the module, and failing if module isn't enabled.
 *
 * @param <T> The object-type to parse
 * @param <IT> intermediate type, this can be an intermediate type holding name and type inside, or
 *     directly the same as type
 * @param <M> The match module that provides it
 */
public abstract class MatchObjectParser<T, IT, M extends MatchModule>
    extends StringLikeParser<CommandSender, T>
    implements BlockingSuggestionProvider.Strings<CommandSender> {

  private final Class<T> objType;
  private final Class<M> moduleType;
  private final String moduleName;

  public MatchObjectParser(
      CommandManager<CommandSender> manager,
      ParserParameters options,
      Class<T> objType,
      Class<M> moduleType,
      String moduleName) {
    super(manager, options);
    this.objType = objType;
    this.moduleType = moduleType;
    this.moduleName = moduleName;
  }

  @Override
  public ArgumentParseResult<T> parse(
      @NotNull CommandContext<CommandSender> context, @NotNull String text) {
    Match match = CommandUtils.getMatch(context);
    if (match == null) return failure(playerOnly());

    M module = match.getModule(moduleType);
    if (module == null) return failure(moduleNotFound());

    IT obj = StringUtils.bestFuzzyMatch(text, objects(module), this::getName);
    if (obj == null) return failure(invalidFormat(text, objType));

    return ArgumentParseResult.success(getValue(obj));
  }

  @Override
  public @NotNull List<@NotNull String> stringSuggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull CommandInput input) {
    Match match = CommandUtils.getMatch(context);
    if (match == null) return Collections.emptyList();

    String text = StringUtils.getText(input);
    String mustKeepArg = StringUtils.getMustKeepArg(input);
    String mustKeepTxt = StringUtils.suggestionToText(mustKeepArg);

    return match
        .moduleOptional(moduleType)
        .map(module -> StreamUtils.of(this.objects(module)))
        .orElse(Stream.of())
        .map(this::getName)
        .filter(name -> LiquidMetal.match(name, text))
        .map(name -> StringUtils.getSuggestion(name, mustKeepArg, mustKeepTxt))
        .collect(Collectors.toList());
  }

  protected abstract Iterable<IT> objects(M module);

  protected abstract String getName(IT obj);

  protected abstract T getValue(IT obj);

  protected TextException moduleNotFound() {
    return exception("command.moduleNotFound", text(moduleName));
  }

  public abstract static class Simple<T, M extends MatchModule> extends MatchObjectParser<T, T, M> {
    public Simple(
        CommandManager<CommandSender> manager,
        ParserParameters options,
        Class<T> objType,
        Class<M> moduleType,
        String moduleName) {
      super(manager, options, objType, moduleType, moduleName);
    }

    @Override
    protected T getValue(T obj) {
      return obj;
    }
  }
}
