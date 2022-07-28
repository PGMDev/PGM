package tc.oc.pgm.filters.matcher.player;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

public class PlayerMovementFilter extends ParticipantFilter {
  private final boolean sprinting;
  private final boolean sneaking;

  public PlayerMovementFilter(boolean sprinting, boolean sneaking) {
    this.sprinting = sprinting;
    this.sneaking = sneaking;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(PlayerCoarseMoveEvent.class, PlayerToggleSneakEvent.class);
  }

  @Override
  public boolean matches(PlayerQuery query, MatchPlayer player) {
    return sprinting == player.getBukkit().isSprinting()
        && sneaking == player.getBukkit().isSneaking();
  }
}
