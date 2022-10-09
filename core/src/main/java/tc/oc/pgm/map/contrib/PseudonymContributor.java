package tc.oc.pgm.map.contrib;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.base.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.util.named.NameStyle;

public class PseudonymContributor implements Contributor {

  private final String name;
  private final @Nullable String contribution;

  public PseudonymContributor(String name, @Nullable String contribution) {
    this.name = assertNotNull(name);
    this.contribution = contribution;
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
  public Component getName(NameStyle style) {
    return text(getNameLegacy(), NamedTextColor.DARK_AQUA);
  }

  @Override
  public String getNameLegacy() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PseudonymContributor)) return false;
    final PseudonymContributor o = (PseudonymContributor) obj;
    return this.name.equals(o.name) && Objects.equal(this.contribution, o.contribution);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + (int) this.name.hashCode();
    hash = 31 * hash + (contribution == null ? 0 : contribution.hashCode());
    return hash;
  }

  @Override
  public String toString() {
    return "PseudonymContributor{name=" + this.name + "}";
  }
}
