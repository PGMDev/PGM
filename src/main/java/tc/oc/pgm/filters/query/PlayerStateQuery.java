package tc.oc.pgm.filters.query;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.MatchPlayerState;
import tc.oc.pgm.match.Party;

public class PlayerStateQuery extends Query implements IPlayerQuery {

  protected final MatchPlayerState playerState;

  public PlayerStateQuery(MatchPlayerState playerState) {
    this(null, playerState);
  }

  public PlayerStateQuery(@Nullable Event event, MatchPlayerState playerState) {
    super(event);
    this.playerState = checkNotNull(playerState);
  }

  @Override
  public Match getMatch() {
    return playerState.getMatch();
  }

  @Override
  public Party getParty() {
    return playerState.getParty();
  }

  @Override
  public UUID getPlayerId() {
    return playerState.getPlayerId();
  }

  @Override
  public @Nullable MatchPlayer getPlayer() {
    return playerState.getMatchPlayer();
  }

  @Override
  public Class<? extends Entity> getEntityType() {
    return Player.class;
  }

  @Override
  public Location getLocation() {
    return playerState.getLocation();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlayerStateQuery)) return false;
    PlayerStateQuery query = (PlayerStateQuery) o;
    if (!playerState.equals(query.playerState)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return playerState.hashCode();
  }
}
