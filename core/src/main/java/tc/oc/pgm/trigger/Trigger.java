package tc.oc.pgm.trigger;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;

public interface Trigger<T> {

  Class<T> getTriggerType();

  void trigger(T t);

  interface TPlayer extends TriggerDefinition<MatchPlayer> {
    @Override
    default Class<MatchPlayer> getTriggerType() {
      return MatchPlayer.class;
    }
  }

  interface TParty extends TriggerDefinition<Party> {
    @Override
    default Class<Party> getTriggerType() {
      return Party.class;
    }
  }

  interface TMatch extends TriggerDefinition<Match> {
    @Override
    default Class<Match> getTriggerType() {
      return Match.class;
    }
  }
}
