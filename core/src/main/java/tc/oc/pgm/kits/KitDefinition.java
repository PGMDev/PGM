package tc.oc.pgm.kits;

import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.trigger.TriggerDefinition;

/** A {@link Kit} that is not a reference */
public interface KitDefinition extends Kit, TriggerDefinition<MatchPlayer> {}
