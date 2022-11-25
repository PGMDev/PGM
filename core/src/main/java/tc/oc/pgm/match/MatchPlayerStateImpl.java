package tc.oc.pgm.match;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

public class MatchPlayerStateImpl implements MatchPlayerState {
  private final @NotNull Match match;
  private final @NotNull String username;
  private final @NotNull UUID uuid;
  private final @NotNull Party party;
  private final boolean dead;
  private final boolean vanished;
  private final @Nullable String nick;

  // Excluded from equals/hashcode
  private final @NotNull Vector location;
  private final @NotNull Audience audience;

  protected MatchPlayerStateImpl(@NotNull MatchPlayer player) {
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
  public @NotNull Match getMatch() {
    return match;
  }

  @Override
  public @NotNull Party getParty() {
    return party;
  }

  @Override
  public @NotNull UUID getId() {
    return uuid;
  }

  @Override
  public @NotNull Location getLocation() {
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
  @NotNull
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MatchPlayerStateImpl)) return false;

    MatchPlayerStateImpl that = (MatchPlayerStateImpl) o;

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
