package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.invalidFormat;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
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

public final class TeamParser extends StringLikeParser<CommandSender, Team> {

  public TeamParser(PaperCommandManager<CommandSender> manager, ParserParameters options) {
    super(manager, options);
  }

  @Override
  public ArgumentParseResult<Team> parse(
      @NotNull CommandContext<CommandSender> context, @NotNull String text) {
    final Match match = PGM.get().getMatchManager().getMatch(context.getSender());
    if (match == null) return failure(playerOnly());

    return getTeam(match, text);
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(
      @NonNull CommandContext<CommandSender> context, @NonNull String input) {
    return getTeams(context.getSender())
        .filter(str -> LiquidMetal.match(str, input))
        .collect(Collectors.toList());
  }

  public static ArgumentParseResult<Team> getTeam(Match match, String text) {
    final TeamMatchModule teams = match.getModule(TeamMatchModule.class);
    if (teams == null) return failure(exception("command.noTeams"));

    final Team team = teams.bestFuzzyMatch(text);
    if (team == null) return failure(invalidFormat(text, Team.class, null));

    return success(team);
  }

  public static Stream<String> getTeams(CommandSender sender) {
    final Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match == null) return Stream.empty();

    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    if (tmm == null) return Stream.empty();

    return tmm.getParticipatingTeams().stream()
        .map(Team::getNameLegacy)
        .map(name -> name.replace(" ", ""));
  }
}
