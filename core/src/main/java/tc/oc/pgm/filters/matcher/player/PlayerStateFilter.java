package tc.oc.pgm.filters.matcher.player;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.spawns.events.PlayerSpawnEvent;

public class PlayerStateFilter extends ParticipantFilter {

  public static final PlayerStateFilter ALIVE = new PlayerStateFilter(true);
  public static final PlayerStateFilter DEAD = new PlayerStateFilter(false);

  private final boolean alive;

  public PlayerStateFilter(boolean alive) {
    this.alive = alive;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(PlayerSpawnEvent.class, MatchPlayerDeathEvent.class);
  }

  @Override
  protected boolean matches(PlayerQuery query, MatchPlayer player) {
    return this.alive ? player.isAlive() : player.isDead();
  }
}
