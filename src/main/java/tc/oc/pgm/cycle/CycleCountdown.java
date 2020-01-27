package tc.oc.pgm.cycle;

import java.util.Objects;
import net.md_5.bungee.api.ChatColor;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.countdowns.MatchCountdown;

public class CycleCountdown extends MatchCountdown {

  private int preloadTime;
  private boolean ended;
  private MapInfo nextMap;
  private MatchFactory nextMatch;

  public CycleCountdown(Match match) {
    super(match);
    this.preloadTime = Config.Experiments.get().getMatchPreLoadSeconds();
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
      if (map != null && nextMatch == null && remaining.getStandardSeconds() <= preloadTime) {
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
    if (!remaining.isLongerThan(Duration.ZERO)) {
      cycleComponent =
          mapName != null
              ? new PersonalizedTranslatable("countdown.cycle.complete", mapName)
              : new PersonalizedTranslatable("countdown.cycle.complete.no_map");
    } else {
      Component secs = secondsRemaining(ChatColor.DARK_RED);
      cycleComponent =
          mapName != null
              ? new PersonalizedTranslatable("countdown.cycle.message", mapName, secs)
              : new PersonalizedTranslatable("countdown.cycle.message.no_map", secs);
    }

    return new PersonalizedText(cycleComponent, ChatColor.DARK_AQUA);
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
