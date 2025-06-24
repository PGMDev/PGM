package tc.oc.pgm.util.attribute;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.attribute.Attribute;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public class Attributes {

  private static final Map<String, Attribute> BY_NAME = new HashMap<>(Attribute.values().length);

  static {
    for (Attribute value : Attribute.values()) {
      if (value != null) BY_NAME.put(StringUtils.simplify(value.name()), value);
    }
  }

  private static Attribute parse(String... names) {
    Attribute type = BukkitUtils.parse(Attribute::valueOf, names);
    for (String name : names) {
      BY_NAME.put(StringUtils.simplify(name), type);
    }
    return type;
  }

  public static Attribute getByName(String name) {
    return BY_NAME.get(StringUtils.simplify(name));
  }
}
