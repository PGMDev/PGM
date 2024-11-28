package tc.oc.pgm.platform.modern.inventory;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.google.common.collect.SetMultimap;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.meta.ItemMeta;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.attribute.AttributeUtils;
import tc.oc.pgm.util.inventory.Slot;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

@Supports(value = PAPER, minVersion = "1.21.1")
@SuppressWarnings("UnstableApiUsage")
public class ModernAttributeUtil implements AttributeUtils {

  @Override
  public AttributeModifier parseModifier(Element el) throws InvalidXMLException {
    var key = el.getAttributeValue("key");
    if (key == null) key = UUID.randomUUID().toString();
    double amount = XMLUtils.parseNumber(Node.fromRequiredAttr(el, "amount"), Double.class);
    var operation = XMLUtils.parseAttributeOperation(Node.fromAttr(el, "operation"));
    var slot = parseSlotGroup(Node.fromAttr(el, "slot"));
    return new AttributeModifier(new NamespacedKey("pgm", key), amount, operation, slot);
  }

  private EquipmentSlotGroup parseSlotGroup(Node node) throws InvalidXMLException {
    if (node == null) return EquipmentSlotGroup.ANY;
    String key = node.getValueNormalize();
    var result = EquipmentSlotGroup.getByName(key);
    if (result != null) return result;

    var slot = Slot.forKey(key);
    if (slot != null && slot.isEquipment()) return slot.toEquipmentSlot().getGroup();

    throw new InvalidXMLException("Unknown attribute slot '" + key + "'", node);
  }

  @Override
  public void copyAttributeModifiers(ItemMeta destination, ItemMeta source) {
    var modifiers = source.getAttributeModifiers();
    if (modifiers != null) modifiers.forEach(destination::addAttributeModifier);
  }

  @Override
  public void applyAttributeModifiers(
      SetMultimap<Attribute, AttributeModifier> modifiers, ItemMeta meta) {
    for (var entry : modifiers.entries()) {
      meta.addAttributeModifier(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public boolean attributesEqual(ItemMeta meta1, ItemMeta meta2) {
    var attributes1 = meta1.getAttributeModifiers();
    var attributes2 = meta2.getAttributeModifiers();
    if (attributes1 == null || attributes2 == null) return false;

    if (!attributes1.keySet().equals(attributes2.keySet())) return false;

    for (Attribute attr : attributes1.keySet()) {
      if (modifiersDiffer(attributes1.get(attr), attributes2.get(attr))) return false;
    }
    return true;
  }

  @Override
  public SimpleAttributeModifier simplify(AttributeModifier mod) {
    return new SimpleModifier(mod.getAmount(), mod.getOperation(), mod.getSlotGroup());
  }

  private record SimpleModifier(
      double amount, AttributeModifier.Operation operation, EquipmentSlotGroup group)
      implements SimpleAttributeModifier {
    @Override
    public int compareTo(@NotNull SimpleAttributeModifier obj) {
      if (!(obj instanceof SimpleModifier o)) return 0;
      int res = operation.ordinal() - o.operation.ordinal();
      if (res != 0) return res;
      res = group.toString().compareTo(o.group.toString());
      if (res != 0) return res;

      return Double.compare(amount, o.amount);
    }
  }

  @Override
  public void stripAttributes(ItemMeta meta) {
    var attributes = meta.getAttributeModifiers();

    if (attributes != null && !attributes.isEmpty()) {
      attributes.keySet().forEach(meta::removeAttributeModifier);
    }
  }
}
