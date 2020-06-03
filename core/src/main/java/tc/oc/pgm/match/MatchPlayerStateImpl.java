package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import net.kyori.text.Component;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.chat.MultiAudience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

public class MatchPlayerStateImpl implements MatchPlayerState, MultiAudience {

  private final Match match;
  private final String username;
  private final UUID uuid;
  private final Party party;
  private final Vector location;

  protected MatchPlayerStateImpl(MatchPlayer player) {
    this.match = checkNotNull(player).getMatch();
    this.username = player.getBukkit().getName();
    this.uuid = player.getId();
    this.party = checkNotNull(player.getParty());
    this.location = player.getBukkit().getLocation().toVector();
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
    return PlayerComponent.of(player.getBukkit(), username, style);
  }

  @Override
  public String getNameLegacy() {
    return username;
  }

  @Override
  public Iterable<? extends Audience> getAudiences() {
    return getPlayer().map(Collections::singleton).orElseGet(Collections::emptySet);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(getMatch()).append(getParty()).append(getId()).build();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MatchPlayerState)) return false;
    final MatchPlayerState o = (MatchPlayerState) obj;
    return new EqualsBuilder()
        .append(getMatch(), o.getMatch())
        .append(getParty(), o.getParty())
        .append(getId(), o.getId())
        .isEquals();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", getId())
        .append("party", getParty().getDefaultName())
        .append("match", getMatch().getId())
        .append("location", location)
        .build();
  }
}
