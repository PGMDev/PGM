package tc.oc.pgm.filters.matcher.player;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

public class FlyingFilter extends ParticipantFilter {
  public static final FlyingFilter INSTANCE = new FlyingFilter();

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(PlayerCoarseMoveEvent.class);
  }

  @Override
  protected boolean matches(PlayerQuery query, MatchPlayer player) {
    return player.getBukkit().isFlying();
  }
}
