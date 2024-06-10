package tc.oc.pgm.command.parsers;

import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.util.text.TextException.exception;

import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ParserParameters;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.command.util.CommandUtils;
import tc.oc.pgm.util.StringUtils;

public final class MapInfoParser extends StringLikeParser<CommandSender, MapInfo>
    implements BlockingSuggestionProvider.Strings<CommandSender> {

  private final MapLibrary library;

  public MapInfoParser(CommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options);
    this.library = PGM.get().getMapLibrary();
  }

  @Override
  public @NotNull ArgumentParseResult<MapInfo> parse(
      @NotNull CommandContext<CommandSender> context, @NotNull String text) {
    MapInfo map = null;

    if (text.equals(CURRENT)) {
      Match match = CommandUtils.getMatch(context);
      if (match != null) map = match.getMap();
    } else {
      map = library.getMap(text);
    }

    return map != null
        ? ArgumentParseResult.success(map)
        : ArgumentParseResult.failure(exception("map.notFound"));
  }

  @Override
  public @NotNull List<@NotNull String> stringSuggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull CommandInput input) {

    // Words to keep, as they cannot be replaced (they're not the last arg)
    String keep = StringUtils.getMustKeepText(input);

    return library
        .getMaps(StringUtils.getText(input))
        .map(MapInfo::getName)
        .map(name -> StringUtils.getSuggestion(name, keep))
        .collect(Collectors.toList());
  }
}
