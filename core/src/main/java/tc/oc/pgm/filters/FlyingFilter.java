package tc.oc.pgm.filters;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

public class FlyingFilter extends ParticipantFilter {

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(PlayerCoarseMoveEvent.class);
  }

  @Override
  protected QueryResponse queryPlayer(PlayerQuery query, MatchPlayer player) {
    return QueryResponse.fromBoolean(player.getBukkit().isFlying());
  }
}
