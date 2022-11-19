package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.command.util.CommandUtils;
import tc.oc.pgm.util.LiquidMetal;

/** Parses parties, ie: teams, plus the obs keyword for the observer party */
public final class PartyParser extends StringLikeParser<CommandSender, Party> {

  private final TeamParser teamParser;

  public PartyParser(PaperCommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options);
    this.teamParser = new TeamParser(manager, options);
  }

  @Override
  public ArgumentParseResult<Party> parse(
      @NotNull CommandContext<CommandSender> context, @NotNull String text) {
    final Match match = CommandUtils.getMatch(context);
    if (match == null) return failure(playerOnly());

    if (text.equalsIgnoreCase("obs")) return success(match.getDefaultParty());

    return teamParser.parse(context, text).mapParsedValue(team -> team);
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      @NonNull CommandContext<CommandSender> context, @NonNull String input) {
    List<String> teams = teamParser.suggestions(context, input);
    if (LiquidMetal.match("obs", input)) teams.add("obs");
    return teams;
  }
}
