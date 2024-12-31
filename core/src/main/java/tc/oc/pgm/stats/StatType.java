package tc.oc.pgm.stats;

import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static tc.oc.pgm.stats.StatsMatchModule.damageComponent;
import static tc.oc.pgm.util.text.NumberComponent.number;

import java.util.Locale;
import net.kyori.adventure.text.Component;

public enum StatType {
  KILLS,
  DEATHS,
  ASSISTS,
  KILL_STREAK,
  BEST_KILL_STREAK,
  KILL_DEATH_RATIO,
  LONGEST_BOW_SHOT {
    private final String blocks = key + ".blocks";

    @Override
    public Component makeNumber(Number number) {
      return translatable(blocks, number(number, YELLOW));
    }
  },
  DAMAGE {
    @Override
    public Component makeNumber(Number number) {
      return damageComponent(number.doubleValue(), GREEN);
    }
  };

  public final String key = "match.stats.type." + name().toLowerCase(Locale.ROOT);
  public static final StatType[] VALUES = values();

  public Component makeNumber(Number number) {
    return number(number, this == DEATHS ? RED : GREEN);
  }

  public Component component(StatHolder stats) {
    return translatable(key, makeNumber(stats.getStat(this)));
  }
}
