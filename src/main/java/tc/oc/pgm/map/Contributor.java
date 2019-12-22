package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedPlayer;
import tc.oc.component.types.PersonalizedText;
import tc.oc.identity.Identity;
import tc.oc.identity.RealIdentity;
import tc.oc.named.NameStyle;
import tc.oc.named.Named;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.db.Username;

/**
 * A contributor to a {@link PGMMap}. Can have either or both of a UUID and arbitrary String name.
 * If a UUID is present, it is used to lookup a username when the map loads. The fallback name is
 * only used if the lookup fails, or no UUID is provided (this could be used to credit somebody
 * without a Minecraft account, like mom or Jesus).
 */
public class Contributor implements Named {

  private final @Nullable UUID uuid;
  private final @Nullable String fallbackName;
  private final @Nullable String contribution;
  private @Nullable String cachedName;

  /** Creates a contributor with a name and a contribution. */
  public Contributor(
      @Nullable UUID uuid, @Nullable String fallbackName, @Nullable String contribution) {
    this.uuid = uuid;
    this.fallbackName = fallbackName;
    this.contribution = contribution;

    checkArgument(uuid != null || fallbackName != null);
    loadUsername();
  }

  public void loadUsername() {
    if (uuid == null || cachedName != null) return;

    final Username username = PGM.get().getDatastore().getUsername(uuid);
    if (username.getName() != null) {
      cachedName = username.getName();
    }
  }

  @Override
  public String toString() {
    return getName();
  }

  public @Nullable UUID getUuid() {
    return uuid;
  }

  /** Gets the name of this contributor. */
  public @Nullable String getName() {
    loadUsername();
    return uuid == null ? fallbackName : cachedName;
  }

  public @Nullable UUID getPlayerId() {
    return uuid;
  }

  public @Nullable Identity getIdentity() {
    return uuid == null || cachedName == null ? null : new RealIdentity(getPlayerId(), getName());
  }

  @Override
  public Component getStyledName(NameStyle style) {
    return getIdentity() != null
        ? new PersonalizedPlayer(getIdentity(), style)
        : new PersonalizedText(fallbackName);
  }

  /** @return true only if a username is available */
  public boolean hasName() {
    return this.uuid != null || this.fallbackName != null;
  }

  /** Indicates whether or not this contributor has a specific contribution. */
  public boolean hasContribution() {
    return this.contribution != null;
  }

  /** Gets this contributor's contribution or null if none exists. */
  public @Nullable String getContribution() {
    return this.contribution;
  }

  public static List<Contributor> filterNamed(List<Contributor> contributors) {
    List<Contributor> resolved = new ArrayList<>();
    for (Contributor contributor : contributors) {
      if (contributor.hasName()) {
        resolved.add(contributor);
      }
    }
    return resolved;
  }
}
