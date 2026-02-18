package tc.oc.pgm.action.actions;

import java.util.Objects;
import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.scoreboard.ScoreboardMatchModule;
import tc.oc.pgm.teams.TeamMatchModule;

public class NametagVisibilityAction extends AbstractAction<Party> {
  private final NameTagVisibility visibility;

  public NametagVisibilityAction(NameTagVisibility visibility) {
    super(Party.class);
    this.visibility = visibility;
  }

  @Override
  public void trigger(Party party) {
    var teamModule = party.getMatch().moduleOptional(TeamMatchModule.class);
    var ffaModule = party.getMatch().moduleOptional(FreeForAllMatchModule.class);

    if (teamModule.isPresent()) {
      var team = teamModule.get().getTeam(party.getNameLegacy());

      if (team != null) {
        team.setNameTagVisibility(this.visibility);
        Objects.requireNonNull(party.getMatch().getModule(ScoreboardMatchModule.class))
            .updatePartyScoreboardTeam(party);
      }
    }
    if (ffaModule.isPresent()) {
      party.getMatch().getCompetitors().forEach(competitor -> {
        competitor.setNameTagVisibility(this.visibility);
        Objects.requireNonNull(party.getMatch().getModule(ScoreboardMatchModule.class))
            .updatePartyScoreboardTeam(competitor);
      });
    }
  }
}
