package tc.oc.pgm.platform.modern.filters;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.entity.Pose;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.matcher.player.ParticipantFilter;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

public class CrawlingFilter extends ParticipantFilter {
  public static final FilterDefinition INSTANCE = new CrawlingFilter();

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(PlayerCoarseMoveEvent.class);
  }

  @Override
  protected boolean matches(PlayerQuery query, MatchPlayer player) {
    return player.getBukkit().getPose() == Pose.SWIMMING && !player.getBukkit().isInWater();
  }
}
