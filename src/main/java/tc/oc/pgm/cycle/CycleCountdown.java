package tc.oc.pgm.cycle;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import net.md_5.bungee.api.ChatColor;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.MatchCountdown;

public class CycleCountdown extends MatchCountdown {

  private int preloadTime;
  private boolean ended;
  private MapInfo nextMap;
  private CompletableFuture<Match> nextMatch;

  public CycleCountdown(Match match) {
    super(match);
    this.preloadTime = Config.Experiments.get().getPreload();
  }

  private void setNextMap(MapInfo map, boolean end) {
    if (!ended) {
      if (!Objects.equals(nextMap, map)) {
        nextMap = map;
        nextMatch = null;
      }
      if (map != null && nextMatch == null && remaining.getStandardSeconds() <= preloadTime) {
        nextMatch =
            PGM.get()
                .getMapLibrary()
                .loadExistingMap(map.getId())
                .thenCompose(context -> PGM.get().getMatchFactory().initMatch(context));
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
  public void onEnd(Duration total) {
    super.onEnd(total);
    setNextMap(PGM.get().getMapOrder().popNextMap(), true);

    nextMatch.whenComplete(
        (Match next, Throwable err) -> {
          try {
            if (err != null) throw err;
            PGM.get().getMatchFactory().moveMatch(match, next);
          } catch (Throwable t) {
            PGM.get().getGameLogger().log(Level.SEVERE, "Unable to cycle match", t);
            nextMatch = null;
          }
        });
  }
}
