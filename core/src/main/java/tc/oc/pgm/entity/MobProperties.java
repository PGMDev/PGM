package tc.oc.pgm.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.DyeColor;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Horse;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.material.Colorable;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.entity.MobProperty.Parser;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public abstract class MobProperties {
  protected static final Parser<Boolean> BOOL = XMLUtils::parseBoolean;
  protected static final Parser<Integer> INT = n -> XMLUtils.parseNumber(n, Integer.class);
  protected static final Parser<Double> DOUBLE = n -> XMLUtils.parseNumber(n, Double.class);
  protected static final Parser<Float> FLOAT = n -> XMLUtils.parseNumber(n, Float.class);
  protected static final Parser<String> STRING = Node::getValue;

  protected static <T extends Enum<T>> Parser<T> enumOf(Class<T> type) {
    return n -> XMLUtils.parseEnum(n, type);
  }

  public static final MobProperties MOB_PROPERTIES = Platform.get(MobProperties.class);

  private final List<MobProperty<?, ?>> properties = new ArrayList<>();

  protected MobProperties() {
    register(Entity.class, "custom-name", STRING, Entity::setCustomName);
    register(Entity.class, "custom-name-visible", BOOL, Entity::setCustomNameVisible);
    register(Entity.class, "fire-ticks", INT, Entity::setFireTicks);

    register(LivingEntity.class, "can-pickup-items", BOOL, LivingEntity::setCanPickupItems);
    register(LivingEntity.class, "remove-when-far-away", BOOL, LivingEntity::setRemoveWhenFarAway);
    register(LivingEntity.class, "arrows-stuck", INT, LivingEntity::setArrowsStuck);
    register(LivingEntity.class, "maximum-air", INT, LivingEntity::setMaximumAir);
    register(LivingEntity.class, "remaining-air", INT, LivingEntity::setRemainingAir);
    register(
        LivingEntity.class, "maximum-no-damage-ticks", INT, LivingEntity::setMaximumNoDamageTicks);
    register(LivingEntity.class, "no-damage-ticks", INT, LivingEntity::setNoDamageTicks);

    register(Damageable.class, "max-health", DOUBLE, Damageable::setMaxHealth);
    register(Damageable.class, "health", DOUBLE, Damageable::setHealth);

    register(Ageable.class, "baby", BOOL, (a, baby) -> {
      if (baby) a.setBaby();
      else a.setAdult();
    });
    register(Ageable.class, "age", INT, Ageable::setAge);
    register(Ageable.class, "age-lock", BOOL, Ageable::setAgeLock);

    register(Tameable.class, "tamed", BOOL, Tameable::setTamed);
    register(Colorable.class, "color", enumOf(DyeColor.class), Colorable::setColor);

    register(Slime.class, "size", INT, Slime::setSize);
    register(Sheep.class, "sheared", BOOL, Sheep::setSheared);
    register(Pig.class, "saddle", BOOL, Pig::setSaddle);

    register(Horse.class, "color", enumOf(Horse.Color.class), Horse::setColor);
    register(Horse.class, "style", enumOf(Horse.Style.class), Horse::setStyle);
    register(Horse.class, "carrying-chest", BOOL, Horse::setCarryingChest);
    register(Horse.class, "max-domestication", INT, Horse::setMaxDomestication);
    register(Horse.class, "domestication", INT, Horse::setDomestication);
    register(Horse.class, "jump-strength", DOUBLE, Horse::setJumpStrength);

    register(Wolf.class, "angry", BOOL, Wolf::setAngry);
    register(Wolf.class, "sitting", BOOL, Wolf::setSitting);
    register(Wolf.class, "collar-color", enumOf(DyeColor.class), Wolf::setCollarColor);

    register(Ocelot.class, "cat-type", enumOf(Ocelot.Type.class), Ocelot::setCatType);

    register(Zombie.class, "baby", BOOL, Zombie::setBaby);
    register(Zombie.class, "villager", BOOL, Zombie::setVillager);
    register(PigZombie.class, "anger", INT, PigZombie::setAnger);
    register(PigZombie.class, "angry", BOOL, PigZombie::setAngry);

    register(IronGolem.class, "player-created", BOOL, IronGolem::setPlayerCreated);
    register(Rabbit.class, "rabbit-type", enumOf(Rabbit.Type.class), Rabbit::setRabbitType);
    register(Bat.class, "awake", BOOL, Bat::setAwake);
    register(Guardian.class, "elder", BOOL, Guardian::setElder);
    register(Creeper.class, "powered", BOOL, Creeper::setPowered);

    register(ArmorStand.class, "base-plate", BOOL, ArmorStand::setBasePlate);
    register(ArmorStand.class, "visible", BOOL, ArmorStand::setVisible);
    register(ArmorStand.class, "arms", BOOL, ArmorStand::setArms);
    register(ArmorStand.class, "small", BOOL, ArmorStand::setSmall);
    register(ArmorStand.class, "marker", BOOL, ArmorStand::setMarker);
    register(ArmorStand.class, "can-move", BOOL, ArmorStand::setCanMove);
  }

  protected final <E, V> void register(
      Class<E> ownerType, String xmlName, Parser<V> parser, BiConsumer<E, V> setter) {
    properties.add(new MobProperty<>(ownerType, xmlName, parser, setter));
  }

  public final @Nullable MobProperty<?, ?> find(Class<? extends Entity> type, String xmlName) {
    for (var property : properties) {
      if (property.ownerType().isAssignableFrom(type) && property.xmlName().equals(xmlName)) {
        return property;
      }
    }
    return null;
  }

  public final List<Consumer<Entity>> parseAttributes(
      Class<? extends Entity> type, Element el, String... excluded) throws InvalidXMLException {
    var skip = Set.of(excluded);
    Set<String> matched = new HashSet<>();
    List<Consumer<Entity>> applied = new ArrayList<>();
    // Walk the registry in order, to maintain dependency order, ie maxHealth before health
    for (var property : properties) {
      var name = property.xmlName();
      if (matched.contains(name) || !property.ownerType().isAssignableFrom(type)) continue;
      var attr = el.getAttribute(name);
      if (attr == null) continue;
      applied.add(property.parseValue(new Node(attr)));
      matched.add(name);
    }
    for (var attr : el.getAttributes()) {
      if (!skip.contains(attr.getName()) && !matched.contains(attr.getName())) {
        throw new InvalidXMLException(
            "Attribute '" + attr.getName() + "' is not a valid property for "
                + type.getSimpleName(),
            el);
      }
    }
    return applied;
  }
}
