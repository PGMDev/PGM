package tc.oc.pgm.command.parsers;

import static org.incendo.cloud.parser.ArgumentParseResult.failure;
import static org.incendo.cloud.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ParserParameters;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.command.util.CommandUtils;
import tc.oc.pgm.util.LiquidMetal;

/** Parses parties, ie: teams, plus the obs keyword for the observer party */
public final class PartyParser extends StringLikeParser<CommandSender, Party>
    implements BlockingSuggestionProvider.Strings<CommandSender> {

  private final TeamParser teamParser;

  public PartyParser(CommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options);
    this.teamParser = new TeamParser(manager, options);
  }

  @Override
  public ArgumentParseResult<Party> parse(
      @NotNull CommandContext<CommandSender> context, @NotNull String text) {
    final Match match = CommandUtils.getMatch(context);
    if (match == null) return failure(playerOnly());

    if (text.equalsIgnoreCase("obs")) return success(match.getDefaultParty());

    return teamParser.parse(context, text).mapSuccess(team -> team);
  }

  @Override
  public @NotNull List<@NotNull String> stringSuggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull CommandInput input) {
    final String next = input.peekString();
    List<String> teams = teamParser.stringSuggestions(context, input);
    if (LiquidMetal.match("obs", next)) teams.add("obs");
    return teams;
  }
}
