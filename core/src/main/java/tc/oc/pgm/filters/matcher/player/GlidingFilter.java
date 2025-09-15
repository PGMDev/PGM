package tc.oc.pgm.filters.matcher.player;

import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;
import tc.oc.pgm.util.platform.Platform;

public class GlidingFilter extends ParticipantFilter {
  public static final FilterDefinition INSTANCE =
      Platform.isModern() ? new GlidingFilter() : StaticFilter.DENY;

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(PlayerCoarseMoveEvent.class);
  }

  @Override
  protected boolean matches(PlayerQuery query, MatchPlayer player) {
    return PLAYER_UTILS.isGliding(player.getBukkit());
  }
}
