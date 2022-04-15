package tc.oc.pgm.util.attribute;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.attribute.Attribute;

public interface AttributeParser {

  Map<String, Attribute> ATTRIBUTE_MAP = buildAttributeMap();

  static Map<String, Attribute> buildAttributeMap() {
    Map<String, Attribute> attributeMap = new HashMap<>();

    for (Attribute attribute : Attribute.values()) {
      attributeMap.put(attribute.name().replace(".", "").replace("_", "").toUpperCase(), attribute);
    }

    return attributeMap;
  }

  static Attribute matchAttribute(String text) {
    text = text.toUpperCase().replace(".", "").replace("_", "");
    Attribute attribute = ATTRIBUTE_MAP.get(text);
    if (attribute == null) attribute = ATTRIBUTE_MAP.get("GENERIC" + text);

    return attribute;
  }
}
