package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.LiquidMetal;

public final class TeamsParser extends StringLikeParser<CommandSender, Collection<Team>> {

  public TeamsParser(PaperCommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options);
  }

  @Override
  public ArgumentParseResult<Collection<Team>> parse(
      @NotNull CommandContext<CommandSender> context, @NotNull String text) {
    final Match match = PGM.get().getMatchManager().getMatch(context.getSender());
    if (match == null) return failure(exception("command.onlyPlayers"));

    if (text.equalsIgnoreCase("*")) {
      final TeamMatchModule teams = match.getModule(TeamMatchModule.class);
      if (teams == null) return failure(exception("command.noTeams"));
      return success(teams.getTeams());
    }

    return TeamParser.getTeam(match, text).mapParsedValue(Collections::singleton);
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      @NonNull CommandContext<CommandSender> context, @NonNull String input) {
    return Stream.concat(Stream.of("*"), TeamParser.getTeams(context.getSender()))
        .filter(str -> LiquidMetal.match(str, input))
        .collect(Collectors.toList());
  }
}
