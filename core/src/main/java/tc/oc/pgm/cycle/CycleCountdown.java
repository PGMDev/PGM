package tc.oc.pgm.cycle;

import com.google.common.collect.Range;
import java.time.Duration;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public class CycleCountdown extends MatchCountdown {

  // Number of seconds before a cycle occurs to start loading the next match.
  // This eases stress on the main thread when handling lots of players.
  private int preloadSecs;

  private MapInfo nextMap;
  private MatchFactory nextMatch;

  public CycleCountdown(Match match) {
    super(match);

    try {
      this.preloadSecs =
          TextParser.parseInteger(
              PGM.get()
                  .getConfiguration()
                  .getExperiments()
                  .getOrDefault("match-preload-seconds", "")
                  .toString(),
              Range.atLeast(0));
    } catch (TextException t) {
      // No-op, since this is experimental
    }
  }

  @Override
  protected Component formatText() {
    Component mapName =
        nextMap == null ? null : TextComponent.of(nextMap.getName(), TextColor.AQUA);

    TranslatableComponent cycleComponent;
    if (remaining.isZero()) {
      cycleComponent =
          mapName != null
              ? TranslatableComponent.of("map.cycledMap", mapName)
              : TranslatableComponent.of("map.cycled");
    } else {
      Component secs = secondsRemaining(TextColor.DARK_RED);
      cycleComponent =
          mapName != null
              ? TranslatableComponent.of("map.cycleMap", mapName, secs)
              : TranslatableComponent.of("map.cycle", secs);
    }

    return cycleComponent.color(TextColor.DARK_AQUA);
  }

  @Override
  public void onTick(Duration remaining, Duration total) {
    super.onTick(remaining, total);

    final MapOrder mapOrder = PGM.get().getMapOrder();
    if (remaining.getSeconds() <= preloadSecs) {
      if (nextMatch != null) return;

      nextMap = mapOrder.popNextMap();
      nextMatch = PGM.get().getMatchManager().createMatch(nextMap.getId());
    } else {
      nextMap = mapOrder.getNextMap();
    }
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);
    if (nextMatch != null) nextMatch.await();
  }
}
