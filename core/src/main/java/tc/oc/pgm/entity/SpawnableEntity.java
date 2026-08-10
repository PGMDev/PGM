package tc.oc.pgm.entity;

import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jdom2.Element;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public record SpawnableEntity(
    Class<? extends LivingEntity> entityType, List<Consumer<Entity>> properties, String id) {

  public Entity spawn(Location location) {
    Entity entity = location.getWorld().spawn(location, entityType);
    for (var property : properties) {
      property.accept(entity);
    }
    return entity;
  }

  public static SpawnableEntity parse(Element el) throws InvalidXMLException {
    var type = parseType(Node.fromRequiredAttr(el, "type"));
    String id = el.getAttributeValue("id");
    return new SpawnableEntity(
        type, MobProperties.MOB_PROPERTIES.parseAttributes(type, el, "type", "id"), id);
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
