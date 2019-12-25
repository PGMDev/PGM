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
import tc.oc.named.NicknameRenderer;
import tc.oc.pgm.api.PGM;

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

  /** Creates a contributor with a name and a contribution. */
  public Contributor(
      @Nullable UUID uuid, @Nullable String fallbackName, @Nullable String contribution) {
    this.uuid = uuid;
    this.fallbackName = fallbackName;
    this.contribution = contribution;

    checkArgument(uuid != null || fallbackName != null);

    // Pre-warm cache before matches load to ensure load times are fast
    getName();
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
    return uuid == null ? fallbackName : PGM.get().getDatastoreCache().getUsername(uuid).getName();
  }

  public @Nullable UUID getPlayerId() {
    return uuid;
  }

  public @Nullable Identity getIdentity() {
    if (uuid == null) return null;
    final String name = getName();
    if (name == null) return null;
    return new RealIdentity(getPlayerId(), name);
  }

  @Override
  public Component getStyledName(NameStyle style) {
    final Identity identity = getIdentity();
    return identity == null
        ? new PersonalizedText(
            fallbackName == null ? "Unknown" : fallbackName, NicknameRenderer.OFFLINE_COLOR)
        : new PersonalizedPlayer(identity, style);
  }

  /** @return true only if a username is available */
  public boolean hasName() {
    return getName() != null;
  }

  /** Indicates whether or not this contributor has a specific contribution. */
  public boolean hasContribution() {
    return contribution != null;
  }

  /** Gets this contributor's contribution or null if none exists. */
  public @Nullable String getContribution() {
    return contribution;
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
