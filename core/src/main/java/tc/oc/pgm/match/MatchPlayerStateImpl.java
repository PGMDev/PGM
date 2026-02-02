package tc.oc.pgm.match;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

public class MatchPlayerStateImpl implements MatchPlayerState {
  private final @NonNull Match match;
  private final @NonNull String username;
  private final @NonNull UUID uuid;
  private final @NonNull Party party;
  private final boolean dead;
  private final boolean vanished;
  private final @Nullable String nick;

  // Excluded from equals/hashcode
  private final @NonNull Vector location;
  private final @NonNull Audience audience;

  protected MatchPlayerStateImpl(@NonNull MatchPlayer player) {
    this.match = assertNotNull(player).getMatch();
    this.username = assertNotNull(player.getBukkit().getName());
    this.uuid = assertNotNull(player.getId());
    this.party = assertNotNull(player.getParty());
    this.dead = player.isDead();
    this.vanished = Integration.isVanished(player.getBukkit());
    this.nick = Integration.getNick(player.getBukkit());

    this.location = assertNotNull(player.getBukkit().getLocation().toVector());
    this.audience = player;
  }

  @Override
  public @NonNull Match getMatch() {
    return match;
  }

  @Override
  public @NonNull Party getParty() {
    return party;
  }

  @Override
  public @NonNull UUID getId() {
    return uuid;
  }

  @Override
  public @NonNull Location getLocation() {
    return location.toLocation(match.getWorld());
  }

  @Override
  public Optional<MatchPlayer> getPlayer() {
    return Optional.ofNullable(getParty().getPlayer(getId()));
  }

  @Override
  public Component getName(NameStyle style) {
    return player(this, style);
  }

  @Override
  public String getNameLegacy() {
    return username;
  }

  @Override
  @NonNull
  public Audience audience() {
    return audience;
  }

  @Override
  public boolean isDead() {
    return dead;
  }

  @Override
  public boolean isVanished() {
    return vanished;
  }

  @Override
  @Nullable
  public String getNick() {
    return nick;
  }

  @Override
  public boolean canInteract() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MatchPlayerStateImpl that)) return false;

    if (isDead() != that.isDead()) return false;
    if (isVanished() != that.isVanished()) return false;
    if (!getMatch().equals(that.getMatch())) return false;
    if (!username.equals(that.username)) return false;
    if (!uuid.equals(that.uuid)) return false;
    if (!getParty().equals(that.getParty())) return false;
    return getNick() != null ? getNick().equals(that.getNick()) : that.getNick() == null;
  }

  @Override
  public int hashCode() {
    int result = getMatch().hashCode();
    result = 31 * result + username.hashCode();
    result = 31 * result + uuid.hashCode();
    result = 31 * result + getParty().hashCode();
    result = 31 * result + (isDead() ? 1 : 0);
    result = 31 * result + (isVanished() ? 1 : 0);
    result = 31 * result + (getNick() != null ? getNick().hashCode() : 0);
    return result;
  }
}
