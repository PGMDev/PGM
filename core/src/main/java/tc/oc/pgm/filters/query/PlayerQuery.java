package tc.oc.pgm.filters.query;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;

public class PlayerQuery extends Query implements tc.oc.pgm.api.filter.query.PlayerQuery {

  private final MatchPlayer player;
  private final Location location;
  private final Location blockCenter;

  public PlayerQuery(@Nullable Event event, MatchPlayer player, @Nullable Location location) {
    super(event);
    this.player = assertNotNull(player);
    this.location = location != null ? location : player.getBukkit().getLocation();
    this.blockCenter = this.location.getBlock().getLocation();
  }

  public PlayerQuery(@Nullable Event event, MatchPlayer player) {
    this(event, player, null);
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
  public UUID getId() {
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
    return location;
  }

  @Override
  public Location getBlockCenter() {
    return blockCenter;
  }

  @Nullable
  @Override
  public PlayerInventory getInventory() {
    return this.player.getInventory();
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
