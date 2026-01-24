package tc.oc.pgm.action.actions;

import java.util.logging.Level;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

public class TeamAliasAction extends AbstractAction<Party> {
  protected final String alias;

  public TeamAliasAction(String alias) {
    super(Party.class);
    this.alias = alias;
  }

  @Override
  public void trigger(Party party) {
    // No-op for non-team parties, we don't want people renaming obs
    if (!(party instanceof Team)) return;
    var existingTeam = party.getMatch().needModule(TeamMatchModule.class).getTeam(alias);
    if (existingTeam != null && existingTeam != party) {
      PGM.get()
          .getGameLogger()
          .log(
              Level.WARNING,
              "Team alias '%s' cannot be applied to '%s' as it's already in use"
                  .formatted(alias, party.getName()));
      return;
    }
    party.setName(this.alias);
  }
}
