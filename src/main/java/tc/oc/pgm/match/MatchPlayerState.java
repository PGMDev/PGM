package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.chat.Audience;
import tc.oc.chat.NullAudience;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedPlayer;
import tc.oc.identity.Identity;
import tc.oc.named.NameStyle;

/** Represents a "snapshot" view of a {@link MatchPlayer}. */
public class MatchPlayerState extends MatchEntityState {
  protected final Identity identity;
  protected final Party party;
  protected final Location location;

  public MatchPlayerState(
      Match match, Identity identity, UUID uuid, Party party, Location location) {
    super(match, Player.class, uuid, location, null);
    checkNotNull(identity, "player");
    checkNotNull(party, "party");

    this.identity = identity;
    this.party = party;
    this.location = location;
  }

  public Identity getIdentity() {
    return identity;
  }

  @Override
  public Component getStyledName(NameStyle style) {
    return new PersonalizedPlayer(getIdentity(), style);
  }

  public UUID getPlayerId() {
    return this.identity.getPlayerId();
  }

  public Location getLocation() {
    return location;
  }

  public Party getParty() {
    return this.party;
  }

  /**
   * Return the {@link MatchPlayer} referenced by this state, or null if the player has switched
   * parties or disconnected.
   */
  public @Nullable MatchPlayer getMatchPlayer() {
    return this.party.getPlayer(this.getPlayerId());
  }

  /**
   * Return the {@link Player} referenced by this state, or null if the player has switched parties
   * or disconnected.
   */
  @Override
  public @Nullable Player getEntity() {
    MatchPlayer player = getMatchPlayer();
    return player == null ? null : player.getBukkit();
  }

  public boolean isPlayer(MatchPlayer player) {
    return this.getPlayerId().equals(player.getPlayerId());
  }

  public boolean isPlayer(MatchPlayerState player) {
    return this.getPlayerId().equals(player.getPlayerId());
  }

  public boolean canInteract() {
    return getParty().isParticipating();
  }

  public Audience getAudience() {
    MatchPlayer matchPlayer = getMatchPlayer();
    return matchPlayer == null ? NullAudience.INSTANCE : matchPlayer;
  }

  @Override
  public String toString() {
    return "MatchPlayerState{match="
        + this.match
        + ",player="
        + this.getPlayerId()
        + ",party="
        + this.party
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof MatchPlayerState) {
      MatchPlayerState other = (MatchPlayerState) o;
      return this.match.equals(other.getMatch())
          && this.getPlayerId().equals(other.getPlayerId())
          && this.party.equals(other.getParty());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int result = match.hashCode();
    result = 31 * result + getPlayerId().hashCode();
    result = 31 * result + party.hashCode();
    return result;
  }
}
