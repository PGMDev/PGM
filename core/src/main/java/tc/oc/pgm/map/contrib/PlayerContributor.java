package tc.oc.pgm.map.contrib;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
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
    return new EqualsBuilder()
        .append(getId(), o.getId())
        .append(getContribution(), o.getContribution())
        .build();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(getId()).append(getContribution()).build();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", getId())
        .append("name", username.getNameLegacy())
        .append("desc", getContribution())
        .build();
  }
}
