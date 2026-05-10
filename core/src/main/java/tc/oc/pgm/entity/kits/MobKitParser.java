package tc.oc.pgm.entity.kits;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.kits.AttributeKit;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.kits.PotionKit;
import tc.oc.pgm.util.inventory.ArmorType;
import tc.oc.pgm.util.inventory.Slot;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class MobKitParser {

  private final MapFactory factory;
  private final KitParser kitParser;

  public MobKitParser(MapFactory factory) {
    this.factory = factory;
    this.kitParser = factory.getKits();
  }

  public MobKit parse(Element el) throws InvalidXMLException {
    String id = FeatureDefinitionContext.parseId(el);
    if (id != null && el.getChildren().isEmpty() && el.getAttributes().size() == 1) {
      return parseReference(new Node(el), id);
    }
    MobKit def = parseDefinition(el);
    if (def == null) {
      throw new InvalidXMLException(
          "mob-kit must contain at least one armor piece, item, or effect", el);
    }
    factory.getFeatures().addFeature(el, def);
    return def;
  }

  public @Nullable MobKit parseDefinition(Element el) throws InvalidXMLException {
    List<MobKit> kits = new ArrayList<>();
    collect(kits, parseEquipmentKit(el));
    collect(kits, parseHorseKit(el));
    collect(kits, parsePotionKit(el));
    collect(kits, parseAttributeKit(el));
    return kits.isEmpty() ? null : new CompositeMobKit(kits);
  }

  private static void collect(List<MobKit> kits, @Nullable MobKit kit) {
    if (kit != null) kits.add(kit);
  }

  private @Nullable MobEquipmentKit parseEquipmentKit(Element el) throws InvalidXMLException {
    Map<ArmorType, MobEquipmentKit.Piece> armor = new EnumMap<>(ArmorType.class);
    for (var armorSlot : Slot.Armor.armor().toList()) {
      Element child = el.getChild(armorSlot.armorTypeName());
      if (child != null) {
        armor.put(armorSlot.getArmorType(), parsePiece(child));
      }
    }

    MobEquipmentKit.Piece mainHand = null;
    MobEquipmentKit.Piece offHand = null;
    for (Element itemEl : el.getChildren("item")) {
      Node slotNode = Node.fromRequiredAttr(itemEl, "slot");
      MobEquipmentKit.Piece piece = parsePiece(itemEl);
      switch (slotNode.getValue()) {
        case "mainhand" -> {
          if (mainHand != null) throw new InvalidXMLException("Duplicate mainhand item", itemEl);
          mainHand = piece;
        }
        case "offhand" -> {
          if (Platform.isLegacy()) {
            throw new InvalidXMLException(
                "Off-hand items are not supported on this server version", slotNode);
          }
          if (offHand != null) throw new InvalidXMLException("Duplicate offhand item", itemEl);
          offHand = piece;
        }
        default ->
          throw new InvalidXMLException(
              "Mob item slot must be 'mainhand' or 'offhand', got '" + slotNode.getValue() + "'",
              slotNode);
      }
    }
    if (armor.isEmpty() && mainHand == null && offHand == null) return null;
    return new MobEquipmentKit(armor, mainHand, offHand);
  }

  private @Nullable MobHorseKit parseHorseKit(Element el) throws InvalidXMLException {
    ItemStack saddle = parseHorseStack(el, "saddle");
    ItemStack horseArmor = parseHorseStack(el, "horse-armor");
    ItemStack llamaDecor = parseHorseStack(el, "llama-decor");
    if (saddle == null && horseArmor == null && llamaDecor == null) return null;
    return new MobHorseKit(saddle, horseArmor, llamaDecor);
  }

  private @Nullable MobKit parsePotionKit(Element el) throws InvalidXMLException {
    List<PotionEffect> effects = kitParser.parsePotions(el);
    if (effects.isEmpty()) return null;
    var frozen = ImmutableSet.copyOf(effects);
    return entity -> PotionKit.applyEffects(entity, frozen, true);
  }

  private @Nullable MobKit parseAttributeKit(Element el) throws InvalidXMLException {
    var modifiers = kitParser.parseAttributeModifiers(el);
    if (modifiers.isEmpty()) return null;
    return entity -> AttributeKit.applyModifiers(entity, modifiers);
  }

  private @Nullable ItemStack parseHorseStack(Element parent, String childName)
      throws InvalidXMLException {
    Element child = parent.getChild(childName);
    return child == null ? null : kitParser.parseItem(child, false);
  }

  public @Nullable MobKit parseReferenceProperty(Element el, String name)
      throws InvalidXMLException {
    Attribute attr = el.getAttribute(name);
    if (attr == null) return null;
    return parseReference(new Node(attr), attr.getValue());
  }

  public MobKit parseReference(Node node, String id) {
    return factory
        .getFeatures()
        .addReference(new XMLMobKitReference(factory.getFeatures(), node, id, MobKit.class));
  }

  private MobEquipmentKit.Piece parsePiece(Element el) throws InvalidXMLException {
    var stack = kitParser.parseItem(el, false);
    Attribute dropAttr = el.getAttribute("drop-chance");
    Float dropChance =
        dropAttr == null ? null : XMLUtils.parseNumber(dropAttr, Float.class, Range.closed(0f, 1f));
    return new MobEquipmentKit.Piece(stack, dropChance);
  }
}
