package tc.oc.pgm.map.contrib;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.kyori.adventure.text.Component.text;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.util.named.NameStyle;

public class PseudonymContributor implements Contributor {

  private final String name;
  private final @Nullable String contribution;

  public PseudonymContributor(String name, @Nullable String contribution) {
    this.name = checkNotNull(name);
    this.contribution = contribution;
  }

  @Override
  public @Nullable String getContribution() {
    return contribution;
  }

  @Override
  public boolean isPlayer(UUID id) {
    return false;
  }

  @Override
  public Component getName(NameStyle style) {
    return text(getNameLegacy(), NamedTextColor.DARK_AQUA);
  }

  @Override
  public String toString() {
    return "PseudonymContributor{"
        + "name='"
        + name
        + '\''
        + ", contribution='"
        + contribution
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PseudonymContributor that = (PseudonymContributor) o;
    return name.equals(that.name) && Objects.equals(contribution, that.contribution);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, contribution);
  }

  @Override
  public String getNameLegacy() {
    return name;
  }
}
