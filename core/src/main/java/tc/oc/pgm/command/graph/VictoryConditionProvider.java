package tc.oc.pgm.command.graph;

import static tc.oc.pgm.util.text.TextException.exception;

import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.argument.MissingArgumentException;
import app.ashcon.intake.argument.Namespace;
import app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import app.ashcon.intake.parametric.ProvisionException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.result.VictoryConditions;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.timelimit.TimeLimit;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;

public class VictoryConditionProvider implements BukkitProvider<VictoryCondition> {

  @Override
  public String getName() {
    return "victory condition";
  }

  @Nullable
  @Override
  public VictoryCondition get(
      CommandSender sender, CommandArgs args, List<? extends Annotation> list)
      throws MissingArgumentException, ProvisionException {
    final String text = args.hasNext() ? args.next() : null;

    final Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match == null) {
      throw exception("command.onlyPlayers");
    }

    // Default to current tl victory condition
    if (text == null) {
      final TimeLimitMatchModule time = match.needModule(TimeLimitMatchModule.class);
      final TimeLimit existing = time.getTimeLimit();
      return existing == null ? null : existing.getResult();
    }

    return VictoryConditions.parse(match, text);
  }

  private static final List<String> BASE_SUGGESTIONS =
      Arrays.asList("default", "tie", "objectives", "score");

  @Override
  public List<String> getSuggestions(
      String prefix,
      CommandSender sender,
      Namespace namespace,
      List<? extends Annotation> modifiers) {
    final Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match == null) return BASE_SUGGESTIONS;

    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    if (tmm == null) return BASE_SUGGESTIONS;

    return Stream.concat(
            BASE_SUGGESTIONS.stream(),
            tmm.getParticipatingTeams().stream()
                .map(Team::getNameLegacy)
                .map(name -> name.replace(" ", "")))
        .collect(Collectors.toList());
  }
}
