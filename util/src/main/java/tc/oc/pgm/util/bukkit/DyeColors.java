package tc.oc.pgm.util.bukkit;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.DyeColor;
import tc.oc.pgm.util.StringUtils;

public class DyeColors {
  private static final Map<String, DyeColor> BY_NAME = new HashMap<>();

  static {
    for (DyeColor value : DyeColor.values()) {
      BY_NAME.put(StringUtils.simplify(value.name()), value);
    }
  }

  public static final DyeColor SILVER = parse("SILVER", "LIGHT_GRAY");

  private static DyeColor parse(String... names) {
    DyeColor type = BukkitUtils.parse(DyeColor::valueOf, names);
    for (String name : names) {
      BY_NAME.put(StringUtils.simplify(name), type);
    }
    return type;
  }

  public static DyeColor getByName(String name) {
    return BY_NAME.get(StringUtils.simplify(name));
  }
}
