package tc.oc.pgm.match;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.text.PlayerComponent.player;

import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
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
    this.match = assertNotNull(player).getMatch();
    this.username = player.getBukkit().getName();
    this.uuid = player.getId();
    this.party = assertNotNull(player.getParty());
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
  @NotNull
  public Audience audience() {
    return audience;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + this.getId().hashCode();
    hash = 31 * hash + this.getParty().hashCode();
    hash = 31 * hash + this.getMatch().hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MatchPlayerState)) return false;
    final MatchPlayerState o = (MatchPlayerState) obj;
    return this.getId().equals(o.getId())
        && this.getParty().equals(o.getParty())
        && this.getMatch().equals(o.getMatch());
  }

  @Override
  public String toString() {
    return "MatchPlayerState{id="
        + this.getId()
        + ", party="
        + this.getParty().getDefaultName()
        + ", match="
        + this.getMatch().getId()
        + ", location="
        + this.location
        + "}";
  }
}
