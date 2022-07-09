package tc.oc.pgm.kits;

import tc.oc.pgm.action.ActionDefinition;
import tc.oc.pgm.api.player.MatchPlayer;

/** A {@link Kit} that is not a reference */
public interface KitDefinition extends Kit, ActionDefinition<MatchPlayer> {}
