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
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.rotation.MapOrder;

public class CycleCountdown extends MatchCountdown {
  private int preloadTime = Config.Experiments.get().getPreload();

  private final MapLibrary mapLibrary;
  private final MatchFactory matchFactory;
  private final MapOrder mapOrder;

  private boolean ended;
  private MapInfo nextMap;
  private CompletableFuture<Match> nextMatch;

  public CycleCountdown(
      MatchFactory matchFactory, MapLibrary mapLibrary, MapOrder mapOrder, Match match) {
    super(match);
    this.mapOrder = mapOrder;
    this.mapLibrary = mapLibrary;
    this.matchFactory = matchFactory;
  }

  private void setNextMap(MapInfo map, boolean end) {
    if (!ended) {
      if (!Objects.equals(nextMap, map)) {
        nextMap = map;
        nextMatch = null;
      }
      if (map != null && nextMatch == null && remaining.getStandardSeconds() <= preloadTime) {
        nextMatch = mapLibrary.loadExistingMap(map.getId()).thenCompose(matchFactory::initMatch);
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
    setNextMap(mapOrder.getNextMap(), false);
    super.onTick(remaining, total);
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);
    setNextMap(mapOrder.popNextMap(), true);

    nextMatch.whenComplete(
        (Match next, Throwable err) -> {
          try {
            if (err != null) throw err;
            matchFactory.moveMatch(match, next);
          } catch (Throwable t) {
            PGM.get().getLogger().log(Level.SEVERE, "Unable to cycle match", t);
            nextMatch = null;
          }
        });
  }
}
