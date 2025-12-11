package tc.oc.pgm.platform.modern.filters;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerRiptideEvent;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.matcher.player.ParticipantFilter;
import tc.oc.pgm.platform.modern.util.event.player.PlayerRiptideEndEvent;

public class RiptidingFilter extends ParticipantFilter {
  public static final FilterDefinition INSTANCE = new RiptidingFilter();

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(PlayerRiptideEvent.class, PlayerRiptideEndEvent.class);
  }

  @Override
  protected boolean matches(PlayerQuery query, MatchPlayer player) {
    return player.getBukkit().isRiptiding();
  }
}
