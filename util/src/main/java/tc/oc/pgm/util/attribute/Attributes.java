package tc.oc.pgm.util.attribute;

import static tc.oc.pgm.util.attribute.AttributeUtils.ATTRIBUTE_UTILS;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.attribute.Attribute;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public class Attributes {

  private static final Map<String, Attribute> BY_NAME =
      new HashMap<>(ATTRIBUTE_UTILS.getAttributeValues().length);

  static {
    for (Attribute value : ATTRIBUTE_UTILS.getAttributeValues()) {
      if (value != null)
        BY_NAME.put(StringUtils.simplify(ATTRIBUTE_UTILS.getAttributeName(value)), value);
    }
  }

  public static Attribute parse(String... names) {
    Attribute type = BukkitUtils.parse(ATTRIBUTE_UTILS::getAttributeValue, names);
    for (String name : names) {
      BY_NAME.put(StringUtils.simplify(name), type);
    }
    return type;
  }

  public static Attribute getByName(String name) {
    return BY_NAME.get(StringUtils.simplify(name));
  }
}
