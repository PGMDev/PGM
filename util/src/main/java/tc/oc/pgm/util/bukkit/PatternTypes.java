package tc.oc.pgm.util.bukkit;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.block.banner.PatternType;
import tc.oc.pgm.util.StringUtils;

public class PatternTypes {
  private static final Map<String, PatternType> BY_NAME = new HashMap<>();

  static {
    parse("CIRCLE", "CIRCLE_MIDDLE");
    parse("DIAGONAL_UP_LEFT", "DIAGONAL_LEFT_MIRROR");
    parse("DIAGONAL_UP_RIGHT", "DIAGONAL_RIGHT_MIRROR");
    parse("HALF_HORIZONTAL_BOTTOM", "HALF_HORIZONTAL_MIRROR");
    parse("HALF_VERTICAL_RIGHT", "HALF_VERTICAL_MIRROR");
    parse("RHOMBUS", "RHOMBUS_MIDDLE");
    parse("SMALL_STRIPES", "STRIPE_SMALL");

    for (PatternType value : PatternType.values()) {
      BY_NAME.put(StringUtils.simplify(value.name()), value);
    }
  }

  private static PatternType parse(String... names) {
    PatternType type = BukkitUtils.parse(PatternType::valueOf, names);
    for (String name : names) {
      BY_NAME.put(StringUtils.simplify(name), type);
    }
    return type;
  }

  public static PatternType getByName(String name) {
    return BY_NAME.get(StringUtils.simplify(name));
  }
}
