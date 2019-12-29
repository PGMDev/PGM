package tc.oc.pgm.cycle;

import net.md_5.bungee.api.ChatColor;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.map.PGMMap;

public class CycleCountdown extends MatchCountdown {
  private int preloadTime = Config.Experiments.get().getPreload();

  protected final MatchManager mm;
  protected PGMMap nextMap;
  private boolean ended, preloadedNext;

  public CycleCountdown(MatchManager mm, Match match) {
    super(match);
    this.mm = mm;
    this.nextMap = mm.getMapOrder().getNextMap();
  }

  private PGMMap setNextMap(PGMMap map, boolean end) {
    if (!ended && nextMap != map) {
      nextMap = map;
      preloadedNext = false;
    }
    ended |= end;
    return nextMap;
  }

  @Override
  protected Component formatText() {
    Component mapName =
        nextMap == null || nextMap.getInfo() == null
            ? null
            : new PersonalizedText(nextMap.getInfo().name, ChatColor.AQUA);

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
    PGMMap next = setNextMap(mm.getMapOrder().getNextMap(), false);
    super.onTick(remaining, total);

    if (remaining.getStandardSeconds() <= preloadTime && next != null && !preloadedNext) {
      preloadedNext = true;
      try {
        mm.createPreMatchAsync(nextMap);
      } catch (Throwable ignore) {
      }
    }
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);
    this.mm.cycleMatch(this.getMatch(), setNextMap(mm.getMapOrder().popNextMap(), true), false);
  }
}
