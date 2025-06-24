package tc.oc.pgm.stats;

import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;
import static tc.oc.pgm.stats.StatsMatchModule.damageComponent;
import static tc.oc.pgm.util.text.NumberComponent.number;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.math.Formula;

public sealed interface StatType<I> {
  Component makeNumber(Number number);

  Component component(Component valueComponent);

  Component component(I input);

  enum Builtin implements StatType<StatHolder> {
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

    @Override
    public Component makeNumber(Number number) {
      return number(number, this == DEATHS ? RED : GREEN);
    }

    @Override
    public Component component(StatHolder stats) {
      return translatable(key, makeNumber(stats.getStat(this)));
    }

    @Override
    public Component component(Component valueComponent) {
      return translatable(key, valueComponent);
    }
  }

  record OfFormula(Component name, Formula<MatchPlayer> formula, TextColor color)
      implements StatType<MatchPlayer> {
    private static final String KEY = "match.stats.type.generic";

    @Override
    public Component makeNumber(Number number) {
      return number(number).color(color);
    }

    @Override
    public Component component(MatchPlayer player) {
      return translatable(KEY, name, makeNumber(formula.apply(player)));
    }

    @Override
    public Component component(Component valueComponent) {
      return translatable(KEY, name, valueComponent);
    }
  }
}
