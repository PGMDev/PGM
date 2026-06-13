package tc.oc.pgm.entity;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
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

public record SpawnableEntity(
    Class<? extends Entity> entityType, List<Consumer<Entity>> properties, Kit kit) {

  private static final Set<Class<? extends Entity>> EXCLUDED_TYPES =
      Set.of(ComplexEntityPart.class, EnderDragon.class, Player.class, Wither.class);

  public SpawnableEntity(Class<? extends Entity> entityType) {
    this(entityType, List.of(), KitNode.EMPTY);
  }

  public Entity spawn(Location location) {
    Entity entity = location.getWorld().spawn(location, entityType);
    for (var property : properties) {
      property.accept(entity);
    }
    if (entity instanceof LivingEntity living) kit.apply(living);
    return entity;
  }

  public static SpawnableEntity parse(Element el, MapFactory factory) throws InvalidXMLException {
    var type = parseType(Node.fromRequiredAttr(el, "type"));

    List<Consumer<Entity>> properties =
        MobProperties.MOB_PROPERTIES.parseAttributes(type, el, "kit");

    Kit kit = KitNode.EMPTY;
    if (LivingEntity.class.isAssignableFrom(type)) {
      Class<? extends LivingEntity> livingType = type.asSubclass(LivingEntity.class);
      kit = factory.getParser().kit(el, "kit").optional(KitNode.EMPTY);

      // Kit contents can't be inspected until references resolve, so mob compatibility
      // is validated through the feature context, which defers until after resolution
      FeatureValidation<KitDefinition> validation =
          (def, node) -> def.validateMob(livingType, node);
      factory.getFeatures().validate(kit, validation, new Node(el));
    } else if (el.getAttribute("kit") != null) {
      throw new InvalidXMLException(
          "Kits can only be applied to living entities, not " + type.getSimpleName(),
          el.getAttribute("kit"));
    }

    return new SpawnableEntity(type, properties, kit);
  }

  public static Class<? extends Entity> parseType(Node typeNode) throws InvalidXMLException {
    Class<? extends Entity> type = EntityTypes.getClassByName(typeNode.getValueNormalize());
    if (type == null) type = XMLUtils.parseEntityType(typeNode);
    if (isExcluded(type)) {
      throw new InvalidXMLException(
          "Entity type " + type.getSimpleName() + " cannot be spawned", typeNode);
    }
    return type;
  }

  private static boolean isExcluded(Class<? extends Entity> type) {
    return EXCLUDED_TYPES.stream().anyMatch(excluded -> excluded.isAssignableFrom(type));
  }
}
