package tc.oc.pgm.util.attribute;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.attribute.Attribute;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public class Attributes {

  private static final Map<String, Attribute> BY_NAME = new HashMap<>(Attribute.values().length);

  static {
    parse("HORSE_JUMP_STRENGTH", "GENERIC_JUMP_STRENGTH", "JUMP_STRENGTH");
    parse("ZOMBIE_SPAWN_REINFORCEMENTS", "SPAWN_REINFORCEMENTS");

    for (Attribute value : Attribute.values()) {
      if (value != null) BY_NAME.put(simplifyAttributeKey(value.name()), value);
    }
  }

  public static Attribute KNOCKBACK_RESISTANCE =
      parse("GENERIC_KNOCKBACK_RESISTANCE", "KNOCKBACK_RESISTANCE");

  private static Attribute parse(String... names) {
    Attribute type = BukkitUtils.parse(Attribute::valueOf, names);
    for (String name : names) {
      BY_NAME.put(simplifyAttributeKey(name), type);
    }
    return type;
  }

  public static Attribute getByName(String name) {
    return BY_NAME.get(simplifyAttributeKey(name));
  }

  private static String simplifyAttributeKey(String name) {
    var split = name.split("[._]", 2);
    if ("generic".equalsIgnoreCase(split[0]) || "player".equalsIgnoreCase(split[0]))
      name = split.length > 1 ? split[1] : "";
    return StringUtils.simplify(name);
  }
}
