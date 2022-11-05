package tc.oc.pgm.command.parsers;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.command.util.CommandUtils;
import tc.oc.pgm.result.VictoryConditions;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.timelimit.TimeLimit;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.text.TextException;

public final class VictoryConditionParser
    implements ArgumentParser<CommandSender, Optional<VictoryCondition>> {

  @Override
  public @NonNull ArgumentParseResult<@NonNull Optional<VictoryCondition>> parse(
      @NonNull CommandContext<@NonNull CommandSender> context,
      @NonNull Queue<@NonNull String> inputQueue) {
    final String input = inputQueue.poll();
    if (input == null) {
      return failure(new NoInputProvidedException(VictoryConditionParser.class, context));
    }

    final Match match = CommandUtils.getMatch(context);
    if (match == null) return failure(playerOnly());

    // Default to current tl victory condition
    if (input.equals(CURRENT)) {
      final TimeLimitMatchModule time = match.needModule(TimeLimitMatchModule.class);
      final TimeLimit existing = time.getTimeLimit();
      return success(Optional.ofNullable(existing).map(TimeLimit::getResult));
    }

    try {
      return success(VictoryConditions.parseOptional(match, input));
    } catch (TextException e) {
      return failure(e);
    }
  }

  private static final List<String> BASE_SUGGESTIONS =
      Arrays.asList("default", "tie", "objectives", "score");

  @Override
  public @NonNull List<@NonNull String> suggestions(
      @NonNull CommandContext<CommandSender> context, @NonNull String input) {
    final Match match = CommandUtils.getMatch(context);
    if (match == null) return BASE_SUGGESTIONS;

    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    if (tmm == null) return BASE_SUGGESTIONS;

    return Stream.concat(
            BASE_SUGGESTIONS.stream(),
            tmm.getParticipatingTeams().stream()
                .map(Team::getNameLegacy)
                .map(name -> name.replace(" ", "")))
        .filter(str -> LiquidMetal.match(str, input))
        .collect(Collectors.toList());
  }
}
