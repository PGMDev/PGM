package tc.oc.pgm.util.bukkit;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.block.banner.PatternType;
import tc.oc.pgm.util.StringUtils;

public class PatternTypes {
  private static final Map<String, PatternType> BY_NAME = new HashMap<>();

  static {
    for (PatternType value : PatternType.values()) {
      BY_NAME.put(StringUtils.simplify(value.name()), value);
    }
  }

  public static final PatternType CIRCLE = parse("CIRCLE", "CIRCLE_MIDDLE");
  public static final PatternType DIAGONAL_UP_LEFT =
      parse("DIAGONAL_UP_LEFT", "DIAGONAL_LEFT_MIRROR");
  public static final PatternType DIAGONAL_UP_RIGHT =
      parse("DIAGONAL_UP_RIGHT", "DIAGONAL_RIGHT_MIRROR");
  public static final PatternType HALF_HORIZONTAL_BOTTOM =
      parse("HALF_HORIZONTAL_BOTTOM", "HALF_HORIZONTAL_MIRROR");
  public static final PatternType HALF_VERTICAL_RIGHT =
      parse("HALF_VERTICAL_RIGHT", "HALF_VERTICAL_MIRROR");
  public static final PatternType RHOMBUS = parse("RHOMBUS", "RHOMBUS_MIDDLE");
  public static final PatternType SMALL_STRIPES = parse("SMALL_STRIPES", "STRIPE_SMALL");

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
