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
import tc.oc.pgm.command.util.CommandKeys;
import tc.oc.pgm.command.util.CommandUtils;
import tc.oc.pgm.util.StringUtils;

public final class MapInfoParser extends StringLikeParser<CommandSender, MapInfo> {

  private final MapLibrary library;

  public MapInfoParser(PaperCommandManager<CommandSender> manager, ParserParameters options) {
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
  public @NonNull List<@NonNull String> suggestions(
      @NonNull CommandContext<CommandSender> context, @NonNull String input) {
    List<String> inputQueue = context.get(CommandKeys.INPUT_QUEUE);

    // Words to keep, as they cannot be replaced (they're not the last arg)
    String keep = StringUtils.getMustKeepText(inputQueue);

    return library
        .getMaps(StringUtils.getText(inputQueue))
        .map(MapInfo::getName)
        .map(name -> StringUtils.getSuggestion(name, keep))
        .collect(Collectors.toList());
  }
}
