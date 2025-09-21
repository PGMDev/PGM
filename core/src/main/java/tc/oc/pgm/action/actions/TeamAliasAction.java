package tc.oc.pgm.action.actions;

import tc.oc.pgm.api.party.Party;

public class TeamAliasAction extends AbstractAction<Party> {
  protected final String alias;

  public TeamAliasAction(String alias) {
    super(Party.class);
    this.alias = alias;
  }

  @Override
  public void trigger(Party party) {
    party.setName(this.alias);
  }
}
