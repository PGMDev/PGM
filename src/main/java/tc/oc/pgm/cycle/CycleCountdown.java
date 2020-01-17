package tc.oc.pgm.cycle;

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

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class CycleCountdown extends MatchCountdown {
  private int preloadTime = Config.Experiments.get().getPreload();

  private final MapLibrary mapLibrary;
  private final MatchFactory matchFactory;
  protected final MapOrder mapOrder;
  protected MapInfo nextMap;
  private boolean ended;
  private CompletableFuture<Match> nextMatch;

  public CycleCountdown(
      MatchFactory matchFactory, MapLibrary mapLibrary, MapOrder mapOrder, Match match) {
    super(match);
    this.mapOrder = mapOrder;
    this.nextMap = mapOrder.getNextMap();
    this.mapLibrary = mapLibrary;
    this.matchFactory = matchFactory;
  }

  private MapInfo setNextMap(MapInfo map, boolean end) {
    if (!ended && nextMap != map) {
      nextMap = map;
      nextMatch = null;
    }
    ended |= end;
    return nextMap;
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
    MapInfo next = setNextMap(mapOrder.getNextMap(), false);
    super.onTick(remaining, total);

    if (remaining.getStandardSeconds() <= preloadTime && next != null && nextMatch == null) {
      nextMatch =
          mapLibrary
              .loadExistingMap(nextMap.getId())
              .thenComposeAsync(matchFactory::createPreMatch);
    }
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);
    setNextMap(mapOrder.popNextMap(), true);

    try {
      matchFactory.createMatch(nextMatch.join(), getMatch().getPlayers());
    } catch (Throwable t) {
      PGM.get().getGameLogger().log(Level.SEVERE, "Could not cycle to map: " + nextMap.getId(), t);
      nextMatch = null;
    }
  }
}
