package tc.oc.pgm.platform.modern.filters;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerRiptideEvent;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.matcher.player.ParticipantFilter;

public class RiptidingFilter extends ParticipantFilter {
  public static final FilterDefinition INSTANCE = new RiptidingFilter();

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(PlayerRiptideEvent.class);
  }

  @Override
  protected boolean matches(PlayerQuery query, MatchPlayer player) {
    return player.getBukkit().isRiptiding();
  }
}
