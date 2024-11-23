package tc.oc.pgm.score;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.util.named.NameStyle;

public record ScoreConfig(
    int scoreLimit,
    int deathScore,
    int killScore,
    int mercyLimit,
    int mercyLimitMin,
    Display display,
    Filter scoreboardFilter) {

  public enum Display {
    NUMERICAL(null, Integer.MAX_VALUE) {
      @Override
      public Component format(Competitor competitor, int score, int limit) {
        return text()
            .append(text(score, NamedTextColor.WHITE))
            .append(limit > 0 ? text("/", NamedTextColor.DARK_GRAY) : empty())
            .append(limit > 0 ? text(limit, NamedTextColor.GRAY) : empty())
            .append(space())
            .append(competitor.getName(NameStyle.SIMPLE_COLOR))
            .build();
      }
    },
    CIRCLE("\u2B24", 16), // ⬤
    SQUARE("\u2b1b", 16), // ⬛
    PIPE("|", 24);

    public final String symbol;
    public final int max;

    Display(String symbol, int max) {
      this.symbol = symbol;
      this.max = max;
    }

    public Component format(Competitor competitor, int score, int limit) {
      if (score < 0 || score > max || limit > max)
        return NUMERICAL.format(competitor, score, limit);
      return text()
          .append(text("\u2794 ", competitor.getTextColor())) // ➔
          .append(text(symbol.repeat(score), competitor.getTextColor()))
          .append(limit > score ? text(symbol.repeat(limit - score), NamedTextColor.GRAY) : empty())
          .build();
    }
  }
}
