package tc.oc.pgm.map.contrib;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.named.NameStyle;
import tc.oc.pgm.api.map.Contributor;

public class PseudonymContributor implements Contributor {

  private final String name;
  private final @Nullable String contribution;

  public PseudonymContributor(String name, @Nullable String contribution) {
    this.name = checkNotNull(name);
    this.contribution = contribution;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getContribution() {
    return contribution;
  }

  @Override
  public boolean isPlayer(UUID id) {
    return false;
  }

  @Override
  public Component getStyledName(NameStyle style) {
    return new PersonalizedText(getName(), ChatColor.DARK_AQUA);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PseudonymContributor)) return false;
    final PseudonymContributor o = (PseudonymContributor) obj;
    return new EqualsBuilder()
        .append(getName(), o.getName())
        .append(getContribution(), o.getContribution())
        .build();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(getName()).append(getContribution()).build();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("name", getName())
        .append("desc", getContribution())
        .build();
  }
}
