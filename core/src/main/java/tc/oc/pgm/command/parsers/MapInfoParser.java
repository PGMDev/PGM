package tc.oc.pgm.command.parsers;

import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.util.LiquidMetal;

public class MapInfoParser extends StringLikeParser<CommandSender, MapInfo> {

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
      map = library.getMap(text);
    }

    return map != null
        ? ArgumentParseResult.success(map)
        : ArgumentParseResult.failure(exception("map.notFound"));
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      @NonNull CommandContext<CommandSender> context, @NonNull String input) {
    List<String> text = getInput(context.getRawInput(), input);

    return library
        .getMaps(String.join(" ", text))
        .map(MapInfo::getName)
        .map(name -> getSuggestion(text, name))
        .collect(Collectors.toList());
  }

  private List<String> getInput(LinkedList<String> rawInput, String input) {
    if (!options.has(StandardParameters.GREEDY) && !options.has(StandardParameters.FLAG_YIELDING))
      return Collections.singletonList(input);

    List<String> value = new ArrayList<>();

    Iterator<String> it = rawInput.descendingIterator();
    while (it.hasNext()) {
      String val = it.next();
      value.add(val);
      if (val.equals(input)) break;
    }
    Collections.reverse(value);
    return value;
  }

  private String getSuggestion(List<String> input, String value) {
    // At least one of the two has no spaces, algorithm makes no sense.
    if (input.size() <= 1 || !value.contains(" ")) return value.replace(" ", "┈");

    String[] words = value.split(" ");

    StringJoiner joiner = new StringJoiner("┈");

    int maxReplace = input.size() - 1;
    for (int i = 0, j = 0; j < words.length; j++) {
      if (i < maxReplace && LiquidMetal.match(words[j], input.get(i))) i++;
      else joiner.add(words[j]);
    }

    return joiner.toString();
  }
}
