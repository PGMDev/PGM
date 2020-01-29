package tc.oc.pgm.teams;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.result.ImmediateVictoryCondition;

/** Immediate, unconditional victory for an explicit {@link Team}. */
public class TeamVictoryCondition extends ImmediateVictoryCondition {
  private TeamFactory teamDefinition;

  public TeamVictoryCondition(TeamFactory teamDefinition) {
    this.teamDefinition = checkNotNull(teamDefinition);
  }

  public Team getTeam(Match match) {
    return match.needModule(TeamMatchModule.class).getTeam(teamDefinition);
  }

  @Override
  public int compare(Competitor a, Competitor b) {
    return Boolean.compare(
        Teams.getDefinition(b) == this.teamDefinition,
        Teams.getDefinition(a) == this.teamDefinition);
  }

  @Override
  public Component getDescription(Match match) {
    Team team = getTeam(match);
    return new PersonalizedTranslatable(
        team.isNamePlural()
            ? "broadcast.gameOver.teamWinText.plural"
            : "broadcast.gameOver.teamWinText",
        team.getComponentName());
  }
}
