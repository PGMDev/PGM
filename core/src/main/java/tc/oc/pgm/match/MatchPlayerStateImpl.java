package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;
import static tc.oc.pgm.util.text.PlayerComponent.player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

public class MatchPlayerStateImpl implements MatchPlayerState {

  private final Match match;
  private final String username;
  private final UUID uuid;
  private final Party party;
  private final Vector location;
  private final Audience audience;

  protected MatchPlayerStateImpl(MatchPlayer player) {
    this.match = checkNotNull(player).getMatch();
    this.username = player.getBukkit().getName();
    this.uuid = player.getId();
    this.party = checkNotNull(player.getParty());
    this.location = player.getBukkit().getLocation().toVector();
    this.audience = getPlayer().isPresent() ? getPlayer().get() : Audience.empty();
  }

  @Override
  public Match getMatch() {
    return match;
  }

  @Override
  public Party getParty() {
    return party;
  }

  @Override
  public UUID getId() {
    return uuid;
  }

  @Override
  public Location getLocation() {
    return location.toLocation(match.getWorld());
  }

  @Override
  public Optional<MatchPlayer> getPlayer() {
    return Optional.ofNullable(getParty().getPlayer(getId()));
  }

  @Override
  public Component getName(NameStyle style) {
    MatchPlayer player = match.getPlayer(uuid);
    return player(player != null ? player.getBukkit() : null, username, style);
  }

  @Override
  public String getNameLegacy() {
    return username;
  }

  @Override
  @Nonnull
  public Audience audience() {
    return audience;
  }

  @Override
  public String toString() {
    return "MatchPlayerStateImpl{"
        + "match="
        + match
        + ", username='"
        + username
        + '\''
        + ", uuid="
        + uuid
        + ", party="
        + party
        + ", location="
        + location
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatchPlayerStateImpl that = (MatchPlayerStateImpl) o;
    return Objects.equals(match, that.match)
        && Objects.equals(uuid, that.uuid)
        && Objects.equals(party, that.party);
  }

  @Override
  public int hashCode() {
    return Objects.hash(match, uuid, party);
  }
}
