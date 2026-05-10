package tc.oc.pgm.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jdom2.Element;
import tc.oc.pgm.entity.kits.MobKit;
import tc.oc.pgm.entity.kits.MobKitParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public record SpawnableEntity(
    Class<? extends LivingEntity> entityType,
    List<Consumer<Entity>> properties,
    List<MobKit> kits) {

  public Entity spawn(Location location) {
    Entity entity = location.getWorld().spawn(location, entityType);
    for (var property : properties) {
      property.accept(entity);
    }
    return entity;
  }

  public void validate(Element source) throws InvalidXMLException {
    for (MobKit kit : kits) {
      kit.validate(entityType, source);
    }
  }

  public static SpawnableEntity parse(Element el, MobKitParser mobKitParser)
      throws InvalidXMLException {
    var type = parseType(Node.fromRequiredAttr(el, "type"));
    List<Consumer<Entity>> properties =
        new ArrayList<>(MobProperties.MOB_PROPERTIES.parseAttributes(
            type, el, "mob-kit", "attribute", "attributes"));

    List<MobKit> kits = new ArrayList<>();
    MobKit inline = mobKitParser.parseDefinition(el);
    if (inline != null) kits.add(inline);
    MobKit reference = mobKitParser.parseReferenceProperty(el, "mob-kit");
    if (reference != null) kits.add(reference);
    for (MobKit kit : kits) {
      properties.add(e -> kit.apply((LivingEntity) e));
    }

    return new SpawnableEntity(type, properties, kits);
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
