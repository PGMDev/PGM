package tc.oc.pgm.tracker.info;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.util.text.MinecraftComponent;

public class EntityInfo extends OwnerInfoBase implements PhysicalInfo {

  private final EntityType entityType;
  private final Class<? extends Entity> entityClass;
  private final @Nullable String customName;

  public EntityInfo(Entity entity, @Nullable ParticipantState owner) {
    super(owner);
    this.entityType = entity.getType();
    this.entityClass = entity.getClass();
    this.customName = entity.getCustomName();
  }

  public Class<? extends Entity> entityClass() {
    return entityClass;
  }

  @Deprecated
  public Class<? extends Entity> getEntityClass() {
    return entityClass();
  }

  public EntityType entityType() {
    return entityType;
  }

  @Deprecated
  public EntityType getEntityType() {
    return entityType();
  }

  public @Nullable String customName() {
    return customName;
  }

  @Deprecated
  public @Nullable String getCustomName() {
    return customName();
  }

  @Override
  public String identifier() {
    return entityType().name();
  }

  @Override
  public Component name() {
    String customName = customName();
    if (customName != null) {
      return LegacyComponentSerializer.legacySection().deserialize(customName);
    } else {
      return MinecraftComponent.entity(entityType());
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{entity="
        + entityType()
        + " name="
        + customName()
        + " owner="
        + owner()
        + "}";
  }
}
