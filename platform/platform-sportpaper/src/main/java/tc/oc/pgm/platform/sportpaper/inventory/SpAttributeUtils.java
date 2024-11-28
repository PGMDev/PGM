package tc.oc.pgm.platform.sportpaper.inventory;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import com.google.common.collect.SetMultimap;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.meta.ItemMeta;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.attribute.AttributeUtils;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

@Supports(SPORTPAPER)
public class SpAttributeUtils implements AttributeUtils {

  @Override
  public AttributeModifier parseModifier(Element el) throws InvalidXMLException {
    double amount = XMLUtils.parseNumber(Node.fromRequiredAttr(el, "amount"), Double.class);
    var operation = XMLUtils.parseAttributeOperation(Node.fromAttr(el, "operation"));
    return new AttributeModifier("FromXML", amount, operation);
  }

  @Override
  public void copyAttributeModifiers(ItemMeta destination, ItemMeta source) {
    for (String attribute : source.getModifiedAttributes()) {
      for (org.bukkit.attribute.AttributeModifier modifier :
          source.getAttributeModifiers(attribute)) {
        destination.addAttributeModifier(attribute, modifier);
      }
    }
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
    var attributes = meta1.getModifiedAttributes();
    if (!attributes.equals(meta2.getModifiedAttributes())) return false;

    for (String attr : attributes) {
      if (modifiersDiffer(meta1.getAttributeModifiers(attr), meta2.getAttributeModifiers(attr)))
        return false;
    }
    return true;
  }

  @Override
  public SimpleAttributeModifier simplify(AttributeModifier modifier) {
    return new SimpleModifier(modifier);
  }

  private record SimpleModifier(double amount, AttributeModifier.Operation operation)
      implements SimpleAttributeModifier {
    public SimpleModifier(AttributeModifier modifier) {
      this(modifier.getAmount(), modifier.getOperation());
    }

    @Override
    public int compareTo(@NotNull SimpleAttributeModifier obj) {
      if (!(obj instanceof SimpleModifier o)) return 0;
      int res = operation.ordinal() - o.operation.ordinal();
      if (res != 0) return res;
      return Double.compare(amount, o.amount);
    }
  }

  @Override
  public void stripAttributes(ItemMeta meta) {
    for (String attr : meta.getModifiedAttributes()) {
      meta.getAttributeModifiers(attr).clear();
    }
  }
}
