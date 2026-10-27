package tc.oc.pgm.entity;

import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jdom2.Element;
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
 * @param entityType the entity class, used for spawning, validation and property lookup
 */
public record SpawnableEntity(
    Class<? extends Entity> entityType, List<Consumer<Entity>> properties, Kit kit) {

  public Entity spawn(Location location) {
    Entity entity = location.getWorld().spawn(location, entityType);
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
   */
  public static SpawnableEntity parse(Element el, MapFactory factory, boolean livingOnly)
      throws InvalidXMLException {
    Class<? extends Entity> type = parseType(Node.fromRequiredAttr(el, "type"), livingOnly);

    List<Consumer<Entity>> properties =
        MobProperties.MOB_PROPERTIES.parseAttributes(type, el, "kit");

    Kit kit = factory.getParser().kit(el, "kit").optional(KitNode.EMPTY);
    if (kit != KitNode.EMPTY && !LivingEntity.class.isAssignableFrom(type)) {
      throw new InvalidXMLException(
          "Kits can only be given to living entities, not " + type.getSimpleName(), el);
    }
    // Kit contents can't be inspected until references resolve, so mob compatibility
    // is validated through the feature context, which defers until after resolution
    FeatureValidation<KitDefinition> validation = (def, node) -> def.validateMob(type, node);
    factory.getFeatures().validate(kit, validation, new Node(el));

    return new SpawnableEntity(type, properties, kit);
  }

  private static Class<? extends Entity> parseType(Node typeNode, boolean livingOnly)
      throws InvalidXMLException {
    Class<? extends Entity> type;
    try {
      type = XMLUtils.parseEntityType(typeNode);
    } catch (InvalidXMLException e) {
      // Fall back to entity type names, the only way to pick e.g. an oak boat
      EntityType spawnType = EntityTypes.getByName(typeNode.getValue());
      if (spawnType == null || spawnType.getEntityClass() == null) throw e;
      type = spawnType.getEntityClass();
    }
    if (livingOnly && !LivingEntity.class.isAssignableFrom(type)) {
      throw new InvalidXMLException(
          "Mob type must be a living entity, got " + type.getSimpleName(), typeNode);
    }
    if (Player.class.isAssignableFrom(type)) {
      throw new InvalidXMLException(
          "Mob type " + type.getSimpleName() + " cannot be spawned", typeNode);
    }
    return type;
  }
}
