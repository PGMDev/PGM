package tc.oc.pgm.filters.matcher.player;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.match.event.MatchPhaseChangeEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;

public class PlayerStateFilter extends ParticipantFilter {

  public static final PlayerStateFilter ALIVE = new PlayerStateFilter(true);
  public static final PlayerStateFilter DEAD = new PlayerStateFilter(false);

  private final boolean alive;

  public PlayerStateFilter(boolean alive) {
    this.alive = alive;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(
        ParticipantSpawnEvent.class,
        ParticipantDespawnEvent.class,
        MatchPhaseChangeEvent.class,
        PlayerPartyChangeEvent.class);
  }

  @Override
  protected boolean matches(PlayerQuery query, MatchPlayer player) {
    return player.isParticipating() && this.alive != player.isDead();
  }
}
