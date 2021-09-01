package tc.oc.pgm.filters;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

public class GroundedFilter extends ParticipantFilter {

  public static final GroundedFilter INSTANCE = new GroundedFilter();

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(PlayerCoarseMoveEvent.class);
  }

  @Override
  protected QueryResponse queryPlayer(PlayerQuery query, MatchPlayer player) {
    return QueryResponse.fromBoolean(player.getBukkit().isOnGround());
  }
}
