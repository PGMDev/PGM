package tc.oc.pgm.api.filter.query;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.goals.Goal;

/**
 * A query can be thought of as context to a question which can be asked to a {@link Filter}. The
 * context can include different information like a {@link Match}, a {@link Goal}, a {@link
 * MatchPlayer} or a {@link CreatureSpawnEvent.SpawnReason}.
 *
 * @implSpec a {@link Query} interface should only add <b>one</b> piece of information to the
 *     context. To increase the amount of information retrievable from the query it can extend other
 *     queries.
 *     <p>A {@link PlayerQuery} adds a {@link MatchPlayer} to the context, but can also provide info
 *     found in other queries like a {@link Party} or a {@link Location}, therefore it extends those
 *     queries({@link PartyQuery}, {@link LocationQuery}) instead of adding more methods to its own
 *     interface.
 * @see Filter
 */
public interface Query {
  /** The event providing the query information, if any */
  @Nullable
  default Event getEvent() {
    return null;
  }
}
