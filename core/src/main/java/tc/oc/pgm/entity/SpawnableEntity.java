package tc.oc.pgm.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitDefinition;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.kits.XMLKitReference;
import tc.oc.pgm.util.inventory.EntityEquipmentUtil;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public record SpawnableEntity(
    Class<? extends LivingEntity> entityType, List<Consumer<Entity>> properties, List<Kit> kits) {

  public Entity spawn(Location location) {
    Entity entity = location.getWorld().spawn(location, entityType);
    for (var property : properties) {
      property.accept(entity);
    }
    for (Kit kit : kits) {
      kit.apply((LivingEntity) entity);
    }
    return entity;
  }

  public static SpawnableEntity parse(Element el, MapFactory factory) throws InvalidXMLException {
    var type = parseType(Node.fromRequiredAttr(el, "type"));
    KitParser kitParser = factory.getKits();

    List<Consumer<Entity>> properties =
        new ArrayList<>(MobProperties.MOB_PROPERTIES.parseAttributes(type, el, "kit"));
    parseHorseEquipment(el, type, kitParser, properties);

    List<Kit> kits = new ArrayList<>();
    var kitAttr = el.getAttribute("kit");
    if (kitAttr != null) {
      kits.add(kitParser.parseReference(new Node(kitAttr), kitAttr.getValue()));
    }
    for (Element kitEl : el.getChildren("kit")) {
      kits.add(kitParser.parse(kitEl));
    }

    // Kit contents can't be inspected until references resolve, so mob compatibility
    // is validated through the feature context, which defers until after resolution
    FeatureValidation<KitDefinition> validation = (def, node) -> def.validateMob(type, node);
    for (Kit kit : kits) {
      if (kit instanceof XMLKitReference ref) factory.getFeatures().validate(ref, validation);
      else if (kit instanceof KitDefinition def)
        factory.getFeatures().validate(def, validation, new Node(el));
    }

    return new SpawnableEntity(type, properties, kits);
  }

  private static void parseHorseEquipment(
      Element el,
      Class<? extends LivingEntity> type,
      KitParser kitParser,
      List<Consumer<Entity>> properties)
      throws InvalidXMLException {
    Element saddleEl = el.getChild("saddle");
    Element armorEl = el.getChild("horse-armor");
    if (saddleEl != null || armorEl != null) {
      if (!Horse.class.isAssignableFrom(type)) {
        throw new InvalidXMLException(
            "saddle and horse-armor require a horse, got " + type.getSimpleName(), el);
      }
      ItemStack saddle = saddleEl == null ? null : kitParser.parseItem(saddleEl, false);
      ItemStack armor = armorEl == null ? null : kitParser.parseItem(armorEl, false);
      properties.add(e -> {
        Horse horse = (Horse) e;
        if (saddle != null) horse.getInventory().setSaddle(saddle);
        if (armor != null) horse.getInventory().setArmor(armor);
      });
    }
    Element decorEl = el.getChild("llama-decor");
    if (decorEl != null) {
      if (!EntityEquipmentUtil.EQUIPMENT.isLlama(type)) {
        throw new InvalidXMLException(
            "llama-decor requires a llama, got " + type.getSimpleName(), el);
      }
      ItemStack decor = kitParser.parseItem(decorEl, false);
      properties.add(e -> EntityEquipmentUtil.EQUIPMENT.setLlamaDecor((LivingEntity) e, decor));
    }
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
