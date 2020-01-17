package tc.oc.pgm.rotation;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;

/**
 * A type of {@link MapOrder} which orders maps randomly, setting aleatory maps for pgm to be able
 * to continue cycling
 */
public class RandomMapOrder implements MapOrder {

  private MatchManager matchManager;
  private MapInfo nextMap;

  public RandomMapOrder(MatchManager matchManager) {
    this.matchManager = matchManager;
    this.nextMap = getRandomMap();
  }

  private MapInfo getRandomMap() {
    Iterator<Match> iterator = matchManager.getMatches().iterator();
    MapInfo current = iterator.hasNext() ? iterator.next().getMap() : null;
    List<MapInfo> maps =
        new ArrayList<>(
            ImmutableList.copyOf(PGM.get().getMapLibrary().getMaps())); // FIXME: performance
    Collections.shuffle(maps);

    for (MapInfo map : maps) {
      if (map != current) return map;
    }
    return maps.get(0);
  }

  @Override
  public MapInfo popNextMap() {
    MapInfo map = nextMap;
    this.nextMap = getRandomMap();
    return map;
  }

  @Override
  public MapInfo getNextMap() {
    return nextMap;
  }

  @Override
  public void setNextMap(MapInfo map) {
    this.nextMap = map;
  }
}
