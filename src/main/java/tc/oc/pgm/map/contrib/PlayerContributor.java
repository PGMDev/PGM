package tc.oc.pgm.map.contrib;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedPlayer;
import tc.oc.component.types.PersonalizedText;
import tc.oc.identity.RealIdentity;
import tc.oc.named.NameStyle;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.player.Username;

public class PlayerContributor implements Contributor {

  private final UUID id;
  private final @Nullable String contribution;
  private @Nullable String name;

  public PlayerContributor(UUID id, @Nullable String contribution) {
    this.id = checkNotNull(id);
    this.contribution = contribution;
    this.name = getName();
  }

  public UUID getId() {
    return id;
  }

  public Username getUsername() {
    return PGM.get().getDatastoreCache().getUsername(id);
  }

  @Override
  public String getName() {
    if (name == null) {
      name = getUsername().getName();
    }
    return name;
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
  public Component getStyledName(NameStyle style) {
    final String name = getName();
    if (name == null) {
      return new PersonalizedText("Unknown", ChatColor.DARK_AQUA);
    }
    return new PersonalizedPlayer(new RealIdentity(id, name), style);
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
        .append("name", name)
        .append("desc", getContribution())
        .build();
  }
}
