package tc.oc.pgm.util.attribute;

import com.google.common.collect.SetMultimap;
import java.util.Collection;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.meta.ItemMeta;
import org.jdom2.Element;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.xml.InvalidXMLException;

public interface AttributeUtils {
  AttributeUtils ATTRIBUTE_UTILS = Platform.get(AttributeUtils.class);

  AttributeModifier parseModifier(Element el) throws InvalidXMLException;

  void copyAttributeModifiers(ItemMeta destination, ItemMeta source);

  void applyAttributeModifiers(SetMultimap<Attribute, AttributeModifier> modifiers, ItemMeta meta);

  boolean attributesEqual(ItemMeta meta1, ItemMeta meta2);

  default boolean modifiersDiffer(
      Collection<AttributeModifier> a, Collection<AttributeModifier> b) {
    if (a.size() != b.size()) return true;
    if (a.isEmpty()) return false;
    // Fast case for single modifier
    if (a.size() == 1) {
      return !simplify(a.iterator().next()).equals(simplify(b.iterator().next()));
    }

    var listA = a.stream().map(this::simplify).sorted().toList();
    var listB = b.stream().map(this::simplify).sorted().toList();
    return !listA.equals(listB);
  }

  SimpleAttributeModifier simplify(AttributeModifier attributeModifier);

  void stripAttributes(ItemMeta meta);

  interface SimpleAttributeModifier extends Comparable<SimpleAttributeModifier> {}
}
