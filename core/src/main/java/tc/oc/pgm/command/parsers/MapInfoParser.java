package tc.oc.pgm.command.parsers;

import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.command.util.CommandGraph;
import tc.oc.pgm.util.LiquidMetal;

public class MapInfoParser extends StringLikeParser<CommandSender, MapInfo> {

  private static final String FAKE_SPACE = "┈", SPACE = " ";

  private final MapLibrary library;
  private final MatchManager matchManager;

  public MapInfoParser(PaperCommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options);
    this.library = PGM.get().getMapLibrary();
    this.matchManager = PGM.get().getMatchManager();
  }

  @Override
  public @NotNull ArgumentParseResult<MapInfo> parse(
      @NotNull CommandContext<CommandSender> context, @NotNull String text) {
    MapInfo map = null;

    if (text.equals(CURRENT)) {
      final Match match = matchManager.getMatch(context.getSender());
      if (match != null) map = match.getMap();
    } else {
      map = library.getMap(suggestionToText(text));
    }

    return map != null
        ? ArgumentParseResult.success(map)
        : ArgumentParseResult.failure(exception("map.notFound"));
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      @NonNull CommandContext<CommandSender> context, @NonNull String input) {
    List<String> inputQueue = context.get(CommandGraph.INPUT_QUEUE);
    if (inputQueue.isEmpty()) inputQueue.add(input);

    // Actual total input from the player
    String text = suggestionToText(String.join(SPACE, inputQueue));

    // Words to keep, as they cannot be replaced (they're not the last arg)
    String keep =
        suggestionToText(String.join(SPACE, inputQueue.subList(0, inputQueue.size() - 1))) + " ";

    return library
        .getMaps(text)
        .map(MapInfo::getName)
        .map(name -> getSuggestion(name, keep))
        .collect(Collectors.toList());
  }

  private String getSuggestion(String suggestion, String input) {
    // At least one of the two has no spaces, algorithm isn't needed.
    if (input.length() > 1 && suggestion.contains(SPACE)) {
      int matchIdx = LiquidMetal.getIndexOf(suggestion, input);

      // Should never happen!
      if (matchIdx == -1)
        throw new IllegalStateException(
            "Suggestion is not matched by input! '" + suggestion + "', '" + input + "'");

      suggestion = suggestion.substring(matchIdx + 1);
    }

    return textToSuggestion(suggestion);
  }

  private String textToSuggestion(String text) {
    return text.replace(SPACE, FAKE_SPACE).replace(":", "");
  }

  private String suggestionToText(String text) {
    return text.replace(FAKE_SPACE, SPACE);
  }
}
