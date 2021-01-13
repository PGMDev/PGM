package tc.oc.pgm.map.contrib;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.util.named.NameStyle;

public class PlayerContributor implements Contributor {

  private final UUID id;
  private final Username username;
  private final @Nullable String contribution;

  public PlayerContributor(UUID id, @Nullable String contribution) {
    this.id = checkNotNull(id);
    this.username = PGM.get().getDatastore().getUsername(id);
    this.contribution = contribution;
  }

  public UUID getId() {
    return id;
  }

  @Override
  public String toString() {
    return "PlayerContributor{"
        + "id="
        + id
        + ", username="
        + username
        + ", contribution='"
        + contribution
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PlayerContributor that = (PlayerContributor) o;
    return id.equals(that.id) && Objects.equals(contribution, that.contribution);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, contribution);
  }

  @Override
  public @Nullable String getContribution() {
    return contribution;
  }

  @Override
  public boolean isPlayer(UUID id) {
    return this.id.equals(id);
  }

  @Override
  public Component getName(NameStyle style) {
    return username.getName(style);
  }

  @Override
  public String getNameLegacy() {
    return username.getNameLegacy();
  }
}
