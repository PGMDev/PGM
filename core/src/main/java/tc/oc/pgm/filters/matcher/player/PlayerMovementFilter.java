package tc.oc.pgm.filters.matcher.player;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

public class PlayerMovementFilter extends ParticipantFilter {
  public static final PlayerMovementFilter CROUCHING = new PlayerMovementFilter(null, true);
  public static final PlayerMovementFilter WALKING = new PlayerMovementFilter(false, false);
  public static final PlayerMovementFilter SPRINTING = new PlayerMovementFilter(true, null);

  private final Boolean sprinting;
  private final Boolean sneaking;

  public PlayerMovementFilter(Boolean sprinting, Boolean sneaking) {
    this.sprinting = sprinting;
    this.sneaking = sneaking;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(
        PlayerCoarseMoveEvent.class, PlayerToggleSprintEvent.class, PlayerToggleSneakEvent.class);
  }

  @Override
  public boolean matches(PlayerQuery query, MatchPlayer player) {
    return (sprinting == null || sprinting == player.getBukkit().isSprinting())
        && (sneaking == null || sneaking == player.getBukkit().isSneaking());
  }
}
