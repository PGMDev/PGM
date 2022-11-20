package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.command.util.CommandUtils;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

/** Parses teams, supporting * for all teams */
public final class TeamsParser extends StringLikeParser<CommandSender, Collection<Team>> {

  private final TeamParser teamParser;

  public TeamsParser(PaperCommandManager<CommandSender> manager, ParserParameters options) {
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

    return teamParser.parse(context, text).mapParsedValue(Collections::singleton);
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      @NonNull CommandContext<CommandSender> context, @NonNull String input) {
    List<String> teams = teamParser.suggestions(context, input);
    if ("*".startsWith(input)) teams.add("*");
    return teams;
  }
}
