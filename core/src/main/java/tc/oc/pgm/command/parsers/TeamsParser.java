package tc.oc.pgm.command.parsers;

import static org.incendo.cloud.parser.ArgumentParseResult.failure;
import static org.incendo.cloud.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import java.util.Collection;
import java.util.Collections;
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
import tc.oc.pgm.command.util.CommandUtils;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

/** Parses teams, supporting * for all teams */
public final class TeamsParser extends StringLikeParser<CommandSender, Collection<Team>>
    implements BlockingSuggestionProvider.Strings<CommandSender> {

  private final TeamParser teamParser;

  public TeamsParser(CommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options);
    this.teamParser = new TeamParser(manager, options);
  }

  @Override
  public ArgumentParseResult<Collection<Team>> parse(
      @NotNull CommandContext<CommandSender> context, @NotNull String text) {
    final Match match = CommandUtils.getMatch(context);
    if (match == null) return failure(playerOnly());

    if (text.equalsIgnoreCase("*")) {
      final TeamMatchModule teams = match.getModule(TeamMatchModule.class);
      if (teams == null) return failure(exception("command.noTeams"));
      return success(teams.getTeams());
    }

    return teamParser.parse(context, text).mapSuccess(Collections::singleton);
  }

  @Override
  public @NotNull List<@NotNull String> stringSuggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull CommandInput input) {
    final String next = input.peekString();
    List<String> teams = teamParser.stringSuggestions(context, input);
    if ("*".startsWith(next)) teams.add("*");
    return teams;
  }
}
