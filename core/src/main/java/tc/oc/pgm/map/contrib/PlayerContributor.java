package tc.oc.pgm.map.contrib;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.base.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.util.named.NameStyle;

public class PlayerContributor implements Contributor {

  private final UUID id;
  private final Username username;
  private final @Nullable String contribution;

  public PlayerContributor(UUID id, @Nullable String contribution) {
    this.id = assertNotNull(id);
    this.username = PGM.get().getDatastore().getUsername(id);
    this.contribution = contribution;
  }

  public UUID getId() {
    return id;
  }

  @Override
  public String getContribution() {
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

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PlayerContributor)) return false;
    final PlayerContributor o = (PlayerContributor) obj;
    return this.id.equals(o.getId()) && Objects.equal(this.contribution, o.getContribution());
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + this.id.hashCode();
    hash = 31 * hash + (this.contribution == null ? 0 : this.contribution.hashCode());
    return hash;
  }

  @Override
  public String toString() {
    return "PlayerContributor{id=" + this.id + ", name=" + this.username.getNameLegacy() + "}";
  }
}
