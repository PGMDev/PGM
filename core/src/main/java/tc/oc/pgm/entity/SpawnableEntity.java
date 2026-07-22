package tc.oc.pgm.entity;

import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitDefinition;
import tc.oc.pgm.kits.KitNode;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public record SpawnableEntity(
    Class<? extends LivingEntity> entityType, List<Consumer<Entity>> properties, Kit kit) {

  public Entity spawn(Location location) {
    LivingEntity entity = location.getWorld().spawn(location, entityType);
    for (var property : properties) {
      property.accept(entity);
    }
    kit.apply(entity);
    return entity;
  }

  public static SpawnableEntity parse(Element el, MapFactory factory) throws InvalidXMLException {
    var type = parseType(Node.fromRequiredAttr(el, "type"));

    List<Consumer<Entity>> properties =
        MobProperties.MOB_PROPERTIES.parseAttributes(type, el, "kit");

    Kit kit = factory.getParser().kit(el, "kit").optional(KitNode.EMPTY);

    // Kit contents can't be inspected until references resolve, so mob compatibility
    // is validated through the feature context, which defers until after resolution
    FeatureValidation<KitDefinition> validation = (def, node) -> def.validateMob(type, node);
    factory.getFeatures().validate(kit, validation, new Node(el));

    return new SpawnableEntity(type, properties, kit);
  }

  private static Class<? extends LivingEntity> parseType(Node typeNode) throws InvalidXMLException {
    Class<? extends Entity> raw = XMLUtils.parseEntityType(typeNode);
    if (!LivingEntity.class.isAssignableFrom(raw)) {
      throw new InvalidXMLException(
          "Mob type must be a living entity, got " + raw.getSimpleName(), typeNode);
    }
    if (Player.class.isAssignableFrom(raw)) {
      throw new InvalidXMLException(
          "Mob type " + raw.getSimpleName() + " cannot be spawned", typeNode);
    }
    return raw.asSubclass(LivingEntity.class);
  }
}
