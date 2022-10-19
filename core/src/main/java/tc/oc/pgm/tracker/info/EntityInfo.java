package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.util.text.MinecraftComponent;

public class EntityInfo extends OwnerInfoBase implements PhysicalInfo {

  private final EntityType entityType;
  private final @Nullable String customName;

  public EntityInfo(
      EntityType entityType, @Nullable String customName, @Nullable ParticipantState owner) {
    super(owner);
    this.entityType = assertNotNull(entityType);
    this.customName = customName;
  }

  public EntityInfo(EntityType entityType, @Nullable String customName) {
    this(entityType, customName, null);
  }

  public EntityInfo(Entity entity, @Nullable ParticipantState owner) {
    this(entity.getType(), entity.getCustomName(), owner);
  }

  public EntityInfo(Entity entity) {
    this(entity, null);
  }

  public EntityType getEntityType() {
    return entityType;
  }

  public @Nullable String getCustomName() {
    return customName;
  }

  @Override
  public String getIdentifier() {
    return getEntityType().getName();
  }

  @Override
  public Component getName() {
    if (getCustomName() != null) {
      return LegacyComponentSerializer.legacySection().deserialize(getCustomName());
    } else {
      return MinecraftComponent.entity(getEntityType());
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{entity="
        + getEntityType()
        + " name="
        + getCustomName()
        + " owner="
        + getOwner()
        + "}";
  }
}
