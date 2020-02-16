package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedPlayer;
import tc.oc.identity.Identity;
import tc.oc.named.NameStyle;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.chat.MultiAudience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;

public class MatchPlayerStateImpl implements MatchPlayerState, MultiAudience {

  private final Match match;
  private final Identity identity;
  private final Party party;
  private final Vector location;

  protected MatchPlayerStateImpl(Match match, Identity identity, Party party, Location location) {
    this.match = checkNotNull(match);
    this.identity = checkNotNull(identity);
    this.party = checkNotNull(party);
    checkArgument(
        location.getWorld().equals(match.getWorld()), "location and match world must be the same");
    this.location = checkNotNull(location).toVector();
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
    return identity.getPlayerId();
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
  public Component getStyledName(NameStyle style) {
    return new PersonalizedPlayer(identity, style);
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
