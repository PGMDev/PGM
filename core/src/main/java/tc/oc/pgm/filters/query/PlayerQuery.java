package tc.oc.pgm.filters.query;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;

public class PlayerQuery extends Query implements tc.oc.pgm.api.filter.query.PlayerQuery {

  private final MatchPlayer player;

  public PlayerQuery(@Nullable Event event, MatchPlayer player) {
    super(event);
    this.player = checkNotNull(player);
  }

  @Override
  public Match getMatch() {
    return player.getMatch();
  }

  @Override
  public Party getParty() {
    return player.getCompetitor();
  }

  @Override
  public UUID getPlayerId() {
    return player.getId();
  }

  @Override
  public MatchPlayer getPlayer() {
    return player;
  }

  @Override
  public Class<? extends Entity> getEntityType() {
    return Player.class;
  }

  @Override
  public Location getLocation() {
    return player.getBukkit().getLocation();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlayerQuery)) return false;
    PlayerQuery query = (PlayerQuery) o;
    return Objects.equals(player, query.player);
  }

  @Override
  public int hashCode() {
    return Objects.hash(player);
  }
}
