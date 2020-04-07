package tc.oc.pgm.map.contrib;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.player.Username;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedPlayer;
import tc.oc.util.bukkit.component.types.PersonalizedText;
import tc.oc.util.bukkit.named.NameStyle;

public class PlayerContributor implements Contributor {

  private static final String UNKNOWN = "Unknown";
  private final UUID id;
  private final @Nullable String contribution;
  private @Nullable Username username;

  public PlayerContributor(UUID id, @Nullable String contribution) {
    this.id = checkNotNull(id);
    this.contribution = contribution;
    getUsername(); // Pre-warm username cache
  }

  public Username getUsername() {
    if (username == null) {
      username = PGM.get().getDatastore().getUsername(id);
    }
    return username;
  }

  public UUID getId() {
    return id;
  }

  @Override
  public String getName() {
    final Username username = getUsername();
    final String name = username == null ? null : username.getName();
    return name == null ? UNKNOWN : name;
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
    if (name
        == UNKNOWN) { // Double equals is intentional so a player with that name does not conflict
      return new PersonalizedText(UNKNOWN, ChatColor.DARK_AQUA);
    }
    return new PersonalizedPlayer(Bukkit.getPlayer(id), name, style);
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
        .append("name", username == null ? "<unknown>" : username.getName())
        .append("desc", getContribution())
        .build();
  }
}
