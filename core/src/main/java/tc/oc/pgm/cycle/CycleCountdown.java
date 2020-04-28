package tc.oc.pgm.cycle;

import java.time.Duration;
import java.util.Objects;
import net.md_5.bungee.api.ChatColor;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

public class CycleCountdown extends MatchCountdown {

  private int preloadTime;
  private boolean ended;
  private MapInfo nextMap;
  private MatchFactory nextMatch;

  public CycleCountdown(Match match) {
    super(match);
    try {
      this.preloadTime =
          Integer.parseInt(
              PGM.get()
                  .getConfiguration()
                  .getExperiments()
                  .getOrDefault("match-pre-load", "0")
                  .toString());
    } catch (Throwable t) {
      this.preloadTime = 0;
    }
  }

  private void setNextMap(MapInfo map, boolean end) {
    if (!ended) {
      if (!Objects.equals(nextMap, map)) {
        nextMap = map;
        if (nextMatch != null) {
          nextMatch.cancel(true);
        }
        nextMatch = null;
      }
      if (map != null && nextMatch == null && remaining.getSeconds() <= preloadTime) {
        nextMatch = PGM.get().getMatchManager().createMatch(nextMap.getId());
      }
      if (end && nextMatch != null) {
        nextMatch.await();
      }
    }
    ended |= end;
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
    setNextMap(PGM.get().getMapOrder().getNextMap(), false);
    super.onTick(remaining, total);
  }

  @Override
  public void onCancel(Duration remaining, Duration total) {
    super.onCancel(remaining, total);
    setNextMap(null, true);
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);
    setNextMap(PGM.get().getMapOrder().popNextMap(), true);
  }
}
