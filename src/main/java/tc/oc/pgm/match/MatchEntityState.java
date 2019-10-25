package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.named.Named;

public class MatchEntityState implements Named {
  protected final Match match;
  protected final Class<? extends Entity> entityClass;
  protected final EntityType entityType;
  protected final UUID uuid;
  protected final Location location;
  protected final @Nullable String customName;

  protected MatchEntityState(
      Match match,
      Class<? extends Entity> entityClass,
      UUID uuid,
      Location location,
      @Nullable String customName) {
    this.uuid = checkNotNull(uuid);
    this.match = checkNotNull(match);
    this.entityClass = checkNotNull(entityClass);
    this.location = checkNotNull(location);
    this.customName = customName;

    EntityType type = null;
    for (EntityType t : EntityType.values()) {
      if (t.getEntityClass().isAssignableFrom(entityClass)) {
        type = t;
        break;
      }
    }
    checkArgument(type != null, "Unknown entity class " + entityClass);
    this.entityType = type;
  }

  public static @Nullable MatchEntityState get(Entity entity) {
    Match match = Match.get(entity.getWorld());
    String customName = entity instanceof Player ? null : entity.getCustomName();
    return match == null
        ? null
        : new MatchEntityState(
            match, entity.getClass(), entity.getUniqueId(), entity.getLocation(), customName);
  }

  public Match getMatch() {
    return match;
  }

  public Class<? extends Entity> getEntityClass() {
    return entityClass;
  }

  public EntityType getEntityType() {
    return entityType;
  }

  public UUID getUuid() {
    return uuid;
  }

  public Location getLocation() {
    return location;
  }

  @Override
  public Component getStyledName(NameStyle style) {
    if (customName != null) {
      return new PersonalizedText(customName);
    } else {
      return new PersonalizedTranslatable("entity." + entityType.getName() + ".name");
    }
  }

  public boolean isEntity(Entity entity) {
    return uuid.equals(entity.getUniqueId());
  }

  public @Nullable Entity getEntity() {
    // TODO: If we ever actually use this, make it more efficient
    for (Entity entity : match.getWorld().getEntities()) {
      if (uuid.equals(entity.getUniqueId())) return entity;
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MatchEntityState)) return false;
    MatchEntityState state = (MatchEntityState) o;
    return Objects.equals(uuid, state.uuid) && Objects.equals(match, state.match);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, match);
  }

  @Override
  public String toString() {
    return "MatchEntityState{"
        + "match="
        + match
        + ", type="
        + entityClass
        + ", uuid="
        + uuid
        + '}';
  }
}
