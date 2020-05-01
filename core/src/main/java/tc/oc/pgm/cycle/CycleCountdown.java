package tc.oc.pgm.cycle;

import com.google.common.collect.Range;
import java.time.Duration;
import net.md_5.bungee.api.ChatColor;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
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
        nextMap == null ? null : new PersonalizedText(nextMap.getName(), ChatColor.AQUA);

    PersonalizedTranslatable cycleComponent;
    if (remaining.isZero()) {
      cycleComponent =
          mapName != null
              ? new PersonalizedTranslatable("map.cycledMap", mapName)
              : new PersonalizedTranslatable("map.cycled");
    } else {
      Component secs = secondsRemaining(ChatColor.DARK_RED);
      cycleComponent =
          mapName != null
              ? new PersonalizedTranslatable("map.cycleMap", mapName, secs)
              : new PersonalizedTranslatable("map.cycle", secs);
    }

    return cycleComponent.color(ChatColor.DARK_AQUA);
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
