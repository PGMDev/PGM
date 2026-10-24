package tc.oc.pgm.entity;

import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitDefinition;
import tc.oc.pgm.kits.KitNode;
import tc.oc.pgm.util.bukkit.EntityTypes;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

/**
 * An entity defined in XML that can be spawned, with its properties and (for living entities) a
 * kit.
 *
 * @param entityType the entity class, used for validation and property lookup
 * @param spawnType the exact type to spawn, when the class alone isn't enough (e.g. modern servers
 *     split boats into one type per wood but share the {@code Boat} class). Null spawns by class.
 */
public record SpawnableEntity(
    Class<? extends Entity> entityType,
    @Nullable EntityType spawnType,
    List<Consumer<Entity>> properties,
    Kit kit) {

  public Entity spawn(Location location) {
    Entity entity = spawnType != null
        ? location.getWorld().spawnEntity(location, spawnType)
        : location.getWorld().spawn(location, entityType);
    for (var property : properties) {
      property.accept(entity);
    }
    if (entity instanceof LivingEntity living) kit.apply(living);
    return entity;
  }

  /** Parse a living entity (mob). */
  public static SpawnableEntity parse(Element el, MapFactory factory) throws InvalidXMLException {
    return parse(el, factory, true);
  }

  /**
   * Parse a spawnable entity from an element's {@code type}, property attributes and {@code kit}.
   *
   * @param livingOnly whether non-living entities (boats, minecarts...) are rejected
   * @param excluded other attributes on the element that aren't entity properties
   */
  public static SpawnableEntity parse(
      Element el, MapFactory factory, boolean livingOnly, String... excluded)
      throws InvalidXMLException {
    Node typeNode = Node.fromRequiredAttr(el, "type");
    EntityType spawnType = null;
    Class<? extends Entity> type;
    try {
      type = XMLUtils.parseEntityType(typeNode);
    } catch (InvalidXMLException e) {
      // Fall back to entity type names, which are the only way to pick e.g. an oak boat
      spawnType = EntityTypes.getByName(typeNode.getValue());
      if (spawnType == null || spawnType.getEntityClass() == null) throw e;
      type = spawnType.getEntityClass();
    }
    validateType(type, typeNode, livingOnly);

    String[] skip = new String[excluded.length + 1];
    skip[0] = "kit";
    System.arraycopy(excluded, 0, skip, 1, excluded.length);
    List<Consumer<Entity>> properties =
        MobProperties.MOB_PROPERTIES.parseAttributes(type, el, skip);

    Kit kit = factory.getParser().kit(el, "kit").optional(KitNode.EMPTY);
    if (LivingEntity.class.isAssignableFrom(type)) {
      // Kit contents can't be inspected until references resolve, so mob compatibility
      // is validated through the feature context, which defers until after resolution
      var mobType = type.asSubclass(LivingEntity.class);
      FeatureValidation<KitDefinition> validation = (def, node) -> def.validateMob(mobType, node);
      factory.getFeatures().validate(kit, validation, new Node(el));
    } else if (el.getAttribute("kit") != null) {
      throw new InvalidXMLException(
          "Kits can only be given to living entities, not " + type.getSimpleName(), el);
    }

    return new SpawnableEntity(type, spawnType, properties, kit);
  }

  private static void validateType(Class<? extends Entity> type, Node typeNode, boolean livingOnly)
      throws InvalidXMLException {
    if (livingOnly && !LivingEntity.class.isAssignableFrom(type)) {
      throw new InvalidXMLException(
          "Mob type must be a living entity, got " + type.getSimpleName(), typeNode);
    }
    if (Player.class.isAssignableFrom(type)) {
      throw new InvalidXMLException(
          "Mob type " + type.getSimpleName() + " cannot be spawned", typeNode);
    }
  }
}
